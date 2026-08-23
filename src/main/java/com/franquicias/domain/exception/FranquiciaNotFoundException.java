package com.franquicias.domain.exception;

import java.util.UUID;

public class FranquiciaNotFoundException extends RuntimeException {
    public FranquiciaNotFoundException(UUID id) {
        super("Franquicia no encontrada: " + id);
    }
}
