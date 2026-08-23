package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Franquicia;
import java.util.UUID;

public record FranquiciaResponse(UUID id, String nombre) {
    public static FranquiciaResponse from(Franquicia f) {
        return new FranquiciaResponse(f.id(), f.nombre());
    }
}
