package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.SucursalEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SucursalR2dbcRepository extends ReactiveCrudRepository<SucursalEntity, UUID> {}
