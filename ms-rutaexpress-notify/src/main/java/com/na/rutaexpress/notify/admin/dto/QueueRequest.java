package com.na.rutaexpress.notify.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class QueueRequest {

    @NotBlank(message = "name es obligatorio")
    @Pattern(regexp = NamePattern.REGEX, message = NamePattern.MESSAGE)
    private String name;

    private boolean durable = true;

    /** Opcional: exchange al que van los mensajes rechazados. Si se envia, debe enviarse tambien deadLetterRoutingKey. */
    @Pattern(regexp = NamePattern.REGEX, message = NamePattern.MESSAGE)
    private String deadLetterExchange;

    private String deadLetterRoutingKey;
}
