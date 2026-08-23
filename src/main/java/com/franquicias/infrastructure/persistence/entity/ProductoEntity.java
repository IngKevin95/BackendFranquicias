package com.franquicias.infrastructure.persistence.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("producto")
public record ProductoEntity(
    @Id UUID id,
    @Column("sucursal_id") UUID sucursalId,
    String nombre,
    int stock) {}
