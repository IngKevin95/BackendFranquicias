package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NombreRequest(@NotBlank(message = "nombre no puede estar vacío") String nombre) {}
