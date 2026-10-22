package com.na.rutaexpress.notify.consumer;

import com.na.rutaexpress.notify.config.RabbitProperties;
import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.exception.NonRecoverableException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Decide que hacer con cada mensaje consumido:
 *
 *   exito                       -> ACK (el broker borra el mensaje de la cola)
 *   NonRecoverableException     -> NACK sin requeue -> DLX -> DLQ  (sin reintentos)
 *   otra falla (temporal)       -> reintenta hasta max-attempts; si se agotan -> NACK -> DLQ
 *
 * Los mensajes que van a la DLQ quedan registrados en el log y se conservan en la cola
 * para poder inspeccionarlos desde el panel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AckHandler {

    private final RabbitProperties props;

    /** Trabajo concreto que hace cada consumidor con el mensaje. */
    @FunctionalInterface
    public interface MessageAction {
        void execute() throws Exception;
    }

    public void process(Channel channel, long deliveryTag, String queue,
                        ShipmentEvent event, MessageAction action) throws IOException {

        int maxAttempts = props.getRetry().getMaxAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.execute();
                channel.basicAck(deliveryTag, false);
                log.info("[ACK] queue={} tracking={} intento={}/{}",
                        queue, event.getTrackingCode(), attempt, maxAttempts);
                return;

            } catch (NonRecoverableException e) {
                reject(channel, deliveryTag, queue, event, "error no recuperable: " + e.getMessage());
                return;

            } catch (Exception e) {
                log.warn("[RETRY] queue={} tracking={} intento={}/{} error={}",
                        queue, event.getTrackingCode(), attempt, maxAttempts, e.getMessage());

                if (attempt == maxAttempts) {
                    reject(channel, deliveryTag, queue, event, "reintentos agotados: " + e.getMessage());
                    return;
                }
                pause(props.getRetry().getBackoffMs());
            }
        }
    }

    /** NACK sin requeue: el broker lo envia al dead letter exchange y termina en la DLQ. */
    private void reject(Channel channel, long deliveryTag, String queue,
                        ShipmentEvent event, String reason) throws IOException {
        channel.basicNack(deliveryTag, false, false);
        log.error("[NACK->DLQ] queue={} tracking={} eventId={} motivo={}",
                queue, event.getTrackingCode(), event.getEventId(), reason);
    }

    private void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
