package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

public record ModificarStockRequest(@Min(value = 0, message = "stock no puede ser negativo") int stock) {}
