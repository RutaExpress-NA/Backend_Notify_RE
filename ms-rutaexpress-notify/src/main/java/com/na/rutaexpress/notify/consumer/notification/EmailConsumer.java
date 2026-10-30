package com.na.rutaexpress.notify.consumer.notification;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.na.rutaexpress.notify.config.RabbitProperties;
import com.na.rutaexpress.notify.consumer.AckHandler;
import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.email.EmailService;
import com.na.rutaexpress.notify.exception.NonRecoverableException;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Dominio NOTIFICACIONES: avisa por correo cada cambio de estado de un envio. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConsumer {

    private final AckHandler ackHandler;
    private final RabbitProperties props;
    private final EmailService emailService;

    @RabbitListener(queues = "${app.rabbitmq.queues.email}")
    public void onMessage(ShipmentEvent event, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        ackHandler.process(channel, deliveryTag, props.getQueues().getEmail(), event, () -> {
            validate(event);
            simulateTemporaryFailure(event);

            EmailService.EmailResult result = emailService.send(event);
            log.info("[EMAIL] {} a {} <{}> | estado {} | asunto: {} | vista previa: {}",
                    result.sent() ? "Correo ENVIADO" : "Correo generado (SMTP desactivado)",
                    event.getRecipientName(), event.getRecipientEmail(), event.getNewStatus(),
                    result.subject(), result.preview());
        });
    }

    /** Dato invalido: reintentar no lo arregla -> va directo a la DLQ. */
    private void validate(ShipmentEvent event) {
        String email = event.getRecipientEmail();
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new NonRecoverableException("recipientEmail invalido: " + email);
        }
    }

    /** Solo demo: simula que el servicio de correo esta caido (falla temporal). */
    private void simulateTemporaryFailure(ShipmentEvent event) {
        String prefix = props.getSimulateFailurePrefix();
        if (prefix != null && !prefix.isBlank()
                && event.getTrackingCode() != null && event.getTrackingCode().startsWith(prefix)) {
            throw new IllegalStateException("Servicio de correo no disponible (falla simulada)");
        }
    }
}
