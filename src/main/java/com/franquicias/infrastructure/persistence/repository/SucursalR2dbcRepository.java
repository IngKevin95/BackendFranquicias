package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.SucursalEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface SucursalR2dbcRepository extends ReactiveCrudRepository<SucursalEntity, UUID> {
    Flux<SucursalEntity> findByFranquiciaId(UUID franquiciaId);
}
