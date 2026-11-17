package com.spacebus.ops.api.config.exception;

import org.springframework.validation.FieldError;

public record FieldDataValidation(String field, String message) {

    /** Valor que nem chegou a ser convertido (ex.: data "2026-13-40"): a mensagem técnica do Spring não vai para a tela. */
    public static final String VALOR_INVALIDO = "Informe um valor válido.";

    public FieldDataValidation(FieldError error) {
        this(error.getField(), error.isBindingFailure() ? VALOR_INVALIDO : error.getDefaultMessage());
    }
}
