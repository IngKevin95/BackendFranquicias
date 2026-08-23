package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.Sucursal;
import com.franquicias.infrastructure.persistence.entity.SucursalEntity;
import org.springframework.stereotype.Component;

@Component
public class SucursalMapper {

    public SucursalEntity toEntity(Sucursal domain) {
        return new SucursalEntity(domain.id(), domain.franquiciaId(), domain.nombre());
    }

    public Sucursal toDomain(SucursalEntity entity) {
        return new Sucursal(entity.id(), entity.franquiciaId(), entity.nombre());
    }
}
