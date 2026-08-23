package com.franquicias.domain.exception;

import java.util.UUID;

public class SucursalNotFoundException extends RuntimeException {
    public SucursalNotFoundException(UUID id) {
        super("Sucursal no encontrada: " + id);
    }
}
