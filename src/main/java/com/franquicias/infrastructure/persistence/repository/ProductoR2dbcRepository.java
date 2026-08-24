package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.ProductoEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductoR2dbcRepository extends ReactiveCrudRepository<ProductoEntity, UUID> {
    Flux<ProductoEntity> findBySucursalId(UUID sucursalId);
}
