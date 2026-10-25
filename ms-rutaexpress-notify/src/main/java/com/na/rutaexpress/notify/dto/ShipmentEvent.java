package com.na.rutaexpress.notify.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class ShipmentEvent {

    private String eventId;            // si viene vacio, lo genera notify

    @NotNull(message = "shipmentId es obligatorio")
    private Long shipmentId;

    @NotBlank(message = "trackingCode es obligatorio")
    private String trackingCode;

    @NotBlank(message = "newStatus es obligatorio")
    private String newStatus;          // CREADO, ACEPTADO, EN_BODEGA, EN_RUTA, ENTREGADO, CANCELADO

    private String recipientName;
    private String recipientEmail;

    private LocalDateTime occurredAt;  // si viene vacio, lo completa notify
}
