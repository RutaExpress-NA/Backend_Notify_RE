package com.na.rutaexpress.notify.producer;

import com.na.rutaexpress.notify.config.RabbitProperties;
import com.na.rutaexpress.notify.dto.ShipmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * PRODUCTOR: publica en exchanges. No conoce las colas ni a los consumidores,
 * solo el exchange y la routing key: ahi esta el desacoplamiento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitProperties props;

    /** Evento de cambio de estado -> exchange topic, routing key "shipment.<estado>". */
    public void publishStatusEvent(ShipmentEvent event) {
        String routingKey = "shipment." + event.getNewStatus().toLowerCase();
        rabbitTemplate.convertAndSend(props.getExchanges().getEvents(), routingKey, event);
        log.info("[PRODUCER] Evento publicado exchange={} routingKey={} tracking={}",
                props.getExchanges().getEvents(), routingKey, event.getTrackingCode());
    }

    /** Comando para generar la etiqueta -> exchange direct. */
    public void publishLabelCommand(ShipmentEvent event) {
        rabbitTemplate.convertAndSend(
                props.getExchanges().getCommands(), props.getRouting().getLabel(), event);
        log.info("[PRODUCER] Comando de etiqueta publicado routingKey={} tracking={}",
                props.getRouting().getLabel(), event.getTrackingCode());
    }
}
