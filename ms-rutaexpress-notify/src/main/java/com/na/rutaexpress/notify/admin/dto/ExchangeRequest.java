package com.na.rutaexpress.notify.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ExchangeRequest {

    @NotBlank(message = "name es obligatorio")
    @Pattern(regexp = NamePattern.REGEX, message = NamePattern.MESSAGE)
    private String name;

    @NotNull(message = "type es obligatorio (DIRECT, TOPIC o FANOUT)")
    private ExchangeKind type;

    private boolean durable = true;
}
