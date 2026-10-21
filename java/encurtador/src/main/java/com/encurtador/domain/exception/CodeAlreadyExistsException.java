package com.encurtador.domain.exception;

public class CodeAlreadyExistsException extends RuntimeException {
    public CodeAlreadyExistsException(String message) {
        super(message);
    }
}
