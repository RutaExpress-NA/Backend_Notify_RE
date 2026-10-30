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

//genera el ticket de picking para que bodega prepare el envio aceptado
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseConsumer {

    private final AckHandler ackHandler;
    private final RabbitProperties props;
    private final DocumentService documentService;

    @RabbitListener(queues = "${app.rabbitmq.queues.warehouse}")
    public void onMessage(ShipmentEvent event, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        ackHandler.process(channel, deliveryTag, props.getQueues().getWarehouse(), event, () -> {
            Path pdf = documentService.generatePickingTicket(event);
            log.info("[BODEGA] Ticket de picking generado para {} (estado {}) -> {}",
                    event.getTrackingCode(), event.getNewStatus(), pdf);
        });
    }
}
