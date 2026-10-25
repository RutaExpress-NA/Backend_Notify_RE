package com.na.rutaexpress.notify.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BindingRequest {

    @NotBlank(message = "queue es obligatorio")
    private String queue;

    @NotBlank(message = "exchange es obligatorio")
    private String exchange;

    /** Puede ser vacio (por ejemplo en un exchange fanout), pero debe venir en el JSON. */
    @NotNull(message = "routingKey es obligatorio (puede ser texto vacio)")
    private String routingKey;
}
