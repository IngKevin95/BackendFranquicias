package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearProductoRequest(
    @NotBlank(message = "nombre no puede estar vacío") 
    @Size(max = 255, message = "nombre no puede exceder 255 caracteres") 
    String nombre) {}
