package com.franquicias.domain.exception;

import java.util.UUID;

public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(UUID id) {
        super("Producto no encontrado: " + id);
    }
}
