package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.UsuarioEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioR2dbcRepository extends ReactiveCrudRepository<UsuarioEntity, UUID> {
    Mono<UsuarioEntity> findByUsername(String username);
}
