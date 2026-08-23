package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Sucursal;
import java.util.UUID;

public record SucursalResponse(UUID id, UUID franquiciaId, String nombre) {
    public static SucursalResponse from(Sucursal s) {
        return new SucursalResponse(s.id(), s.franquiciaId(), s.nombre());
    }
}
