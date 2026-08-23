package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CrearProductoRequest(
    @NotBlank(message = "nombre no puede estar vacío") String nombre,
    @Min(value = 0, message = "stock no puede ser negativo") int stock) {}
