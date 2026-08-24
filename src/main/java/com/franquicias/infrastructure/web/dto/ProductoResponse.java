package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Producto;
import java.util.UUID;

public record ProductoResponse(UUID id, UUID sucursalId, String nombre, int stock) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.id(), p.sucursalId(), p.nombre(), p.stock());
    }
}
