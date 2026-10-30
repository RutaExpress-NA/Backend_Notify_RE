package com.na.rutaexpress.notify.consumer.logistics;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.na.rutaexpress.notify.config.RabbitProperties;
import com.na.rutaexpress.notify.consumer.AckHandler;
import com.na.rutaexpress.notify.document.DocumentService;
import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Dominio LOGISTICA: genera la etiqueta / guia de despacho en PDF de un envio aceptado. */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabelConsumer {

    private final AckHandler ackHandler;
    private final RabbitProperties props;
    private final DocumentService documentService;

    @RabbitListener(queues = "${app.rabbitmq.queues.label}")
    public void onMessage(ShipmentEvent event, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        ackHandler.process(channel, deliveryTag, props.getQueues().getLabel(), event, () -> {
            Path pdf = documentService.generateLabel(event);
            log.info("[ETIQUETA] PDF generado para {} (destinatario: {}) -> {}",
                    event.getTrackingCode(), event.getRecipientName(), pdf);
        });
    }
}
