package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.infrastructure.persistence.entity.FranquiciaEntity;
import org.springframework.stereotype.Component;

@Component
public class FranquiciaMapper {

    public FranquiciaEntity toEntity(Franquicia domain) {
        return new FranquiciaEntity(domain.id(), domain.nombre());
    }

    public Franquicia toDomain(FranquiciaEntity entity) {
        return new Franquicia(entity.id(), entity.nombre());
    }
}
