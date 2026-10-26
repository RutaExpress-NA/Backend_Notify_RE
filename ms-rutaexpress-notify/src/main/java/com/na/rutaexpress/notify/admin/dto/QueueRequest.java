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


    @Pattern(regexp = NamePattern.REGEX, message = NamePattern.MESSAGE)
    private String deadLetterExchange;

    private String deadLetterRoutingKey;
}
