package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.ProductoEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ProductoR2dbcRepository extends ReactiveCrudRepository<ProductoEntity, UUID> {}
