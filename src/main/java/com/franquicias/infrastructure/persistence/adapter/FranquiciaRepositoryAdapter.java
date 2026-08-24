package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.FranquiciaMapper;
import com.franquicias.infrastructure.persistence.repository.FranquiciaR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.core.DatabaseClient;

@Component
public class FranquiciaRepositoryAdapter implements FranquiciaRepositoryPort {

    private final FranquiciaR2dbcRepository repository;
    private final FranquiciaMapper mapper;
    private final DatabaseClient databaseClient;

    public FranquiciaRepositoryAdapter(FranquiciaR2dbcRepository repository, FranquiciaMapper mapper, DatabaseClient databaseClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Franquicia> save(Franquicia franquicia) {
        return repository.save(mapper.toEntity(franquicia)).map(mapper::toDomain);
    }

    @Override
    public Mono<Franquicia> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Void> updateNombre(UUID id, String nombre) {
        return databaseClient.sql("UPDATE franquicia SET nombre = :nombre WHERE id = :id")
            .bind("nombre", nombre)
            .bind("id", id)
            .fetch()
            .rowsUpdated()
            .then();
    }
}
