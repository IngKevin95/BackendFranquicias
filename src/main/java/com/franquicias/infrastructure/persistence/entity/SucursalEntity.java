package com.franquicias.infrastructure.persistence.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sucursal")
public record SucursalEntity(
    @Id UUID id,
    @Column("franquicia_id") UUID franquiciaId,
    String nombre) {}
