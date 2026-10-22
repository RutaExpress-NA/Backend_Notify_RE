package com.na.rutaexpress.notify.consumer.logistics;

import com.na.rutaexpress.notify.config.RabbitProperties;
import com.na.rutaexpress.notify.consumer.AckHandler;
import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.exception.NonRecoverableException;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Dominio LOGISTICA: genera la etiqueta (ticket) de un envio aceptado. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabelConsumer {

    private final AckHandler ackHandler;
    private final RabbitProperties props;

    @RabbitListener(queues = "${app.rabbitmq.queues.label}")
    public void onMessage(ShipmentEvent event, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        ackHandler.process(channel, deliveryTag, props.getQueues().getLabel(), event, () -> {
            if (event.getRecipientName() == null || event.getRecipientName().isBlank()) {
                throw new NonRecoverableException("recipientName vacio: no se puede imprimir la etiqueta");
            }
            log.info("[ETIQUETA] Etiqueta generada para {} (destinatario: {})",
                    event.getTrackingCode(), event.getRecipientName());
        });
    }
}
