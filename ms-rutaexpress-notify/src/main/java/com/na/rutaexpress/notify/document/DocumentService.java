package com.na.rutaexpress.notify.document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.exception.InvalidRequestException;
import com.na.rutaexpress.notify.exception.NonRecoverableException;
import com.na.rutaexpress.notify.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

//Arma los documentos del envio a partir del evento:   
// - etiqueta / guia de despacho (PDF 10x15 cm con QR y codigo de barras)
// - ticket de picking para bodega (PDF con codigo de barras y checklist)

@Slf4j
@Service
public class DocumentService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    private static final Pattern UNSAFE = Pattern.compile("[^A-Za-z0-9._-]");

    private final PdfService pdfService;
    private final CodeImageService codeImages;
    private final Path outputDir;
    private final String trackingBaseUrl;

    public DocumentService(PdfService pdfService, CodeImageService codeImages,
                           @Value("${app.documents.output-dir:./documents}") String outputDir,
                           @Value("${app.documents.tracking-base-url:http://localhost:5173/shipments/}") String trackingBaseUrl) {
        this.pdfService = pdfService;
        this.codeImages = codeImages;
        this.trackingBaseUrl = trackingBaseUrl;
        this.outputDir = Path.of(outputDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outputDir);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de documentos: " + this.outputDir, e);
        }
    }

    // ------------------------------------------------------------------ etiqueta

    public Path generateLabel(ShipmentEvent event) {
        require(event.getTrackingCode(), "trackingCode");
        require(event.getRecipientName(), "recipientName");
        require(event.getDeliveryAddress(), "deliveryAddress");

        Map<String, String> values = commonValues(event);
        values.put("QR_SRC", codeImages.qrDataUri(trackingBaseUrl + event.getShipmentId(), 300));
        values.put("BARCODE_SRC", codeImages.barcodeDataUri(event.getTrackingCode(), 700, 120));

        byte[] pdf = pdfService.htmlToPdf(render("templates/etiqueta.html", values));
        return save("etiqueta", event.getTrackingCode(), pdf);
    }

    // ------------------------------------------------------------ ticket de picking

    public Path generatePickingTicket(ShipmentEvent event) {
        require(event.getTrackingCode(), "trackingCode");

        Map<String, String> values = commonValues(event);
        values.put("BARCODE_SRC", codeImages.barcodeDataUri(event.getTrackingCode(), 700, 120));

        byte[] pdf = pdfService.htmlToPdf(render("templates/picking.html", values));
        return save("picking", event.getTrackingCode(), pdf);
    }

    // ------------------------------------------------------------------ descarga

    /** Ubica un documento ya generado. type: "label" o "picking". */
    public Path resolve(String tracking, String type) {
        String prefix = switch (type) {
            case "label" -> "etiqueta";
            case "picking" -> "picking";
            default -> throw new InvalidRequestException("type debe ser 'label' o 'picking'");
        };
        Path file = outputDir.resolve(prefix + "-" + safe(tracking) + ".pdf").normalize();
        if (!file.startsWith(outputDir) || !Files.exists(file)) {
            throw new ResourceNotFoundException("No existe el documento '" + type + "' del envio " + tracking);
        }
        return file;
    }

    // ------------------------------------------------------------------- ayudas

    private Map<String, String> commonValues(ShipmentEvent e) {
        Map<String, String> v = new HashMap<>();
        v.put("TRACKING", esc(e.getTrackingCode()));
        v.put("RECIPIENT", esc(e.getRecipientName()));
        v.put("EMAIL", esc(e.getRecipientEmail()));
        v.put("ADDRESS", esc(e.getDeliveryAddress()));
        v.put("CITY", esc(e.getDeliveryCity()));
        v.put("SERVICE", esc(e.getServiceName() != null ? e.getServiceName() : "Envio"));
        v.put("STATUS", esc(e.getNewStatus()));
        v.put("SHIPMENT_NO", e.getShipmentId() != null ? "#" + e.getShipmentId() : "-");
        v.put("WEIGHT", e.getWeightKg() != null
                ? String.format(Locale.forLanguageTag("es-CL"), "%.1f kg", e.getWeightKg()) : "-");
        LocalDateTime when = e.getOccurredAt() != null ? e.getOccurredAt() : LocalDateTime.now();
        v.put("DATE", when.format(DATE_FMT));
        return v;
    }

    private String render(String templatePath, Map<String, String> values) {
        String html;
        try {
            html = StreamUtils.copyToString(new ClassPathResource(templatePath).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la plantilla " + templatePath, e);
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return html;
    }

    private Path save(String prefix, String tracking, byte[] pdf) {
        Path file = outputDir.resolve(prefix + "-" + safe(tracking) + ".pdf");
        try {
            Files.write(file, pdf);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el documento " + file, e);
        }
        log.info("[DOCUMENTO] {} guardado en {} ({} bytes)", prefix, file, pdf.length);
        return file;
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new NonRecoverableException("faltan datos para generar el documento: " + field);
        }
    }

    private String safe(String tracking) {
        return UNSAFE.matcher(tracking).replaceAll("_");
    }

    /** Escapa texto para incrustarlo en XHTML. */
    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
