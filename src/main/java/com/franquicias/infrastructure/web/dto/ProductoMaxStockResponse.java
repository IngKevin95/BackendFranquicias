package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.ProductoMaxStock;
import java.util.UUID;

public record ProductoMaxStockResponse(UUID sucursalId, String sucursalNombre, ProductoResponse producto) {
    public static ProductoMaxStockResponse from(ProductoMaxStock m) {
        return new ProductoMaxStockResponse(m.sucursalId(), m.sucursalNombre(), ProductoResponse.from(m.producto()));
    }
}
