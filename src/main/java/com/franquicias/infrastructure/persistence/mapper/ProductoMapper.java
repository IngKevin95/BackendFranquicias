package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.Producto;
import com.franquicias.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoEntity toEntity(Producto domain) {
        return new ProductoEntity(domain.id(), domain.sucursalId(), domain.nombre(), domain.stock());
    }

    public Producto toDomain(ProductoEntity entity) {
        return new Producto(entity.id(), entity.sucursalId(), entity.nombre(), entity.stock());
    }
}
