package com.na.rutaexpress.notify.exception;

/**
 * Error que NO tiene sentido reintentar (ej. datos invalidos en el mensaje).
 * El consumidor lo manda directo a la DLQ, sin reintentos.
 */
public class NonRecoverableException extends RuntimeException {

    public NonRecoverableException(String message) {
        super(message);
    }
}
