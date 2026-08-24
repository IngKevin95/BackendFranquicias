package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ModificarStockRequest(
    @NotBlank(message = "El tipo de transacción es requerido")
    @Pattern(regexp = "^(ENTRADA|SALIDA)$", message = "El tipo debe ser ENTRADA o SALIDA")
    String tipo,
    
    @Min(value = 1, message = "La cantidad debe ser mayor a 0") 
    int cantidad
) {}
