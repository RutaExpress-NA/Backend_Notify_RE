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

//Dominio LOGISTICA: avisa a bodega que debe preparar la recepcion de un envio aceptado0
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseConsumer {

    private final AckHandler ackHandler;
    private final RabbitProperties props;

    @RabbitListener(queues = "${app.rabbitmq.queues.warehouse}")
    public void onMessage(ShipmentEvent event, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        ackHandler.process(channel, deliveryTag, props.getQueues().getWarehouse(), event, () -> {
            if (event.getTrackingCode() == null || event.getTrackingCode().isBlank()) {
                throw new NonRecoverableException("trackingCode vacio");
            }
            log.info("[BODEGA] Preparar recepcion del envio {} (estado {})",
                    event.getTrackingCode(), event.getNewStatus());
        });
    }
}
