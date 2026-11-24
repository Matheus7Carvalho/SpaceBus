package com.spacebus.ops.domain.core.exception;

/** O perfil tem acesso ao recurso, mas não a este registro (ex.: motorista de outra viagem). Responde 403. */
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
