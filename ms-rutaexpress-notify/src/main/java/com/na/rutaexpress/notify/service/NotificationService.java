package com.na.rutaexpress.notify.service;

import com.na.rutaexpress.notify.dto.ShipmentEvent;
import com.na.rutaexpress.notify.producer.NotificationProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/** Logica de negocio: decide que mensajes se publican segun el estado del envio. */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProducer producer;

    public void process(ShipmentEvent event) {
        if (event.getEventId() == null || event.getEventId().isBlank()) {
            event.setEventId(UUID.randomUUID().toString());
        }
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(LocalDateTime.now());
        }

        producer.publishStatusEvent(event);

        // Cuando el envio es aceptado, ademas se pide generar su etiqueta.
        if ("ACEPTADO".equalsIgnoreCase(event.getNewStatus())) {
            producer.publishLabelCommand(event);
        }
    }
}
