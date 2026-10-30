package com.na.rutaexpress.notify.email;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.exception.InvalidRequestException;
import com.na.rutaexpress.notify.exception.NonRecoverableException;
import com.na.rutaexpress.notify.exception.ResourceNotFoundException;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Arma el correo HTML de cada estado del envio (plantillas en templates/email/) y lo entrega:
 *   - SIEMPRE guarda una vista previa en la carpeta de documentos (se puede abrir desde el navegador).
 *   - Si el SMTP esta activo (app.mail.enabled=true y SPRING_MAIL_HOST definido), ademas lo envia.
 * Si el SMTP falla, lanza una excepcion: AckHandler reintenta y, si no se logra, manda el mensaje a la DLQ.
 */
@Slf4j
@Service
public class EmailService {

    /** Resultado de procesar un correo. */
    public record EmailResult(boolean sent, String subject, Path preview) {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9._-]");
    private static final Pattern SLUG = Pattern.compile("[a-z-]{3,20}");

    private static final Map<String, String> LABELS = Map.of(
            "CREADO", "Creado",
            "ACEPTADO", "Aceptado",
            "EN_BODEGA", "En bodega",
            "EN_RUTA", "En ruta",
            "ENTREGADO", "Entregado",
            "CANCELADO", "Cancelado");

    private static final Map<String, String> EVENTS = Map.of(
            "CREADO", "Solicitud registrada",
            "ACEPTADO", "Aceptado por un despachador",
            "EN_BODEGA", "Recibido en bodega",
            "EN_RUTA", "En camino a tu dirección",
            "ENTREGADO", "Entregado al destinatario",
            "CANCELADO", "Envío cancelado");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Path outputDir;
    private final String trackingBaseUrl;
    private final String assetsBaseUrl;
    private final String from;
    private final boolean enabled;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        @Value("${app.documents.output-dir:./documents}") String outputDir,
                        @Value("${app.documents.tracking-base-url:http://localhost:5173/shipments/}") String trackingBaseUrl,
                        @Value("${app.mail.assets-base-url:https://raw.githubusercontent.com/ToniC-3PO/miniature-tribble-img/refs/heads/main/email-assets}") String assetsBaseUrl,
                        @Value("${app.mail.from:no-reply@rutaexpress.cl}") String from,
                        @Value("${app.mail.enabled:false}") boolean enabled) {
        this.mailSenderProvider = mailSenderProvider;
        this.trackingBaseUrl = trackingBaseUrl;
        this.assetsBaseUrl = assetsBaseUrl;
        this.from = from;
        this.enabled = enabled;
        this.outputDir = Path.of(outputDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outputDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de documentos: " + this.outputDir, e);
        }
    }

    // ------------------------------------------------------------------ envio

    public EmailResult send(ShipmentEvent event) {
        String status = event.getNewStatus() == null ? "" : event.getNewStatus().toUpperCase(Locale.ROOT);
        String slug = status.toLowerCase(Locale.ROOT).replace('_', '-');

        String html = render(slug, status, event);
        String subject = subjectOf(html, event);
        Path preview = savePreview(event.getTrackingCode(), slug, html);

        boolean sent = false;
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (enabled && sender != null) {
            sendReal(sender, event.getRecipientEmail(), subject, html);
            sent = true;
        } else if (enabled) {
            log.warn("[EMAIL] app.mail.enabled=true pero no hay servidor SMTP (falta SPRING_MAIL_HOST): modo simulado");
        }
        return new EmailResult(sent, subject, preview);
    }

    private void sendReal(JavaMailSender sender, String to, String subject, String html) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo por SMTP: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ vista previa

    /** Ubica la vista previa de un correo ya generado. estado: creado, aceptado, en-bodega, en-ruta, entregado o cancelado. */
    public Path resolvePreview(String tracking, String slug) {
        if (!SLUG.matcher(slug).matches()) {
            throw new InvalidRequestException("estado invalido: use creado, aceptado, en-bodega, en-ruta, entregado o cancelado");
        }
        Path file = outputDir.resolve("email-" + safe(tracking) + "-" + slug + ".html").normalize();
        if (!file.startsWith(outputDir) || !Files.exists(file)) {
            throw new ResourceNotFoundException("No existe el correo '" + slug + "' del envio " + tracking);
        }
        return file;
    }

    // ------------------------------------------------------------------ armado

    private String render(String slug, String status, ShipmentEvent e) {
        ClassPathResource resource = new ClassPathResource("templates/email/" + slug + ".html");
        if (!resource.exists()) {
            throw new NonRecoverableException("no hay plantilla de correo para el estado: " + e.getNewStatus());
        }
        String html;
        try {
            html = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la plantilla de correo " + slug, ex);
        }

        String address = blank(e.getDeliveryAddress()) ? "Dirección no informada" : e.getDeliveryAddress();
        if (!blank(e.getDeliveryCity())) {
            address = address + ", " + e.getDeliveryCity();
        }
        LocalDateTime when = e.getOccurredAt() != null ? e.getOccurredAt() : LocalDateTime.now();

        Map<String, String> v = new HashMap<>();
        v.put("recipientName", esc(blank(e.getRecipientName()) ? "cliente" : e.getRecipientName()));
        v.put("trackingCode", esc(e.getTrackingCode()));
        v.put("statusLabel", LABELS.getOrDefault(status, esc(status)));
        v.put("deliveryAddress", esc(address));
        v.put("eventDescription", EVENTS.getOrDefault(status, esc(status)));
        v.put("occurredAt", when.format(DATE_FMT));
        v.put("trackingUrl", esc(trackingBaseUrl + e.getShipmentId()));
        v.put("assetsBaseUrl", esc(assetsBaseUrl));

        for (Map.Entry<String, String> entry : v.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return html;
    }

    /** El asunto sale del <title> de la plantilla. */
    private String subjectOf(String html, ShipmentEvent e) {
        Matcher m = TITLE.matcher(html);
        if (m.find()) {
            return m.group(1).replaceAll("\\s+", " ").trim();
        }
        return "RutasExpress - tu envío " + e.getTrackingCode();
    }

    private Path savePreview(String tracking, String slug, String html) {
        Path file = outputDir.resolve("email-" + safe(tracking) + "-" + slug + ".html");
        try {
            Files.writeString(file, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la vista previa del correo " + file, e);
        }
        return file;
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private String safe(String tracking) {
        return UNSAFE.matcher(tracking).replaceAll("_");
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
