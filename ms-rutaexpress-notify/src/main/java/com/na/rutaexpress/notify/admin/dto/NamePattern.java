package com.na.rutaexpress.notify.admin.dto;

/** Regla unica para nombres de colas y exchanges. */
public final class NamePattern {

    /** 1 a 100 caracteres: letras, numeros, punto, guion y guion bajo. No puede partir con "amq." (reservado por RabbitMQ). */
    public static final String REGEX = "^(?!amq\\.)[A-Za-z0-9._-]{1,100}$";

    public static final String MESSAGE =
            "Nombre invalido: use 1 a 100 caracteres (letras, numeros, '.', '-', '_') sin espacios y que no parta con 'amq.'";

    private NamePattern() {
    }
}
