package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.FranquiciaEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface FranquiciaR2dbcRepository extends ReactiveCrudRepository<FranquiciaEntity, UUID> {}
