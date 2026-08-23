package com.franquicias.infrastructure.persistence.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("franquicia")
public record FranquiciaEntity(@Id UUID id, String nombre) {}
