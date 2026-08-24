package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.SucursalRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.SucursalMapper;
import com.franquicias.infrastructure.persistence.repository.SucursalR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.core.DatabaseClient;

@Component
public class SucursalRepositoryAdapter implements SucursalRepositoryPort {

    private final SucursalR2dbcRepository repository;
    private final SucursalMapper mapper;
    private final DatabaseClient databaseClient;

    public SucursalRepositoryAdapter(SucursalR2dbcRepository repository, SucursalMapper mapper, DatabaseClient databaseClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Sucursal> save(Sucursal sucursal) {
        return repository.save(mapper.toEntity(sucursal)).map(mapper::toDomain);
    }

    @Override
    public Mono<Sucursal> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Void> updateNombre(UUID id, String nombre) {
        return databaseClient.sql("UPDATE sucursal SET nombre = :nombre WHERE id = :id")
            .bind("nombre", nombre)
            .bind("id", id)
            .fetch()
            .rowsUpdated()
            .then();
    }

    @Override
    public Flux<Sucursal> findByFranquiciaId(UUID franquiciaId) {
        return repository.findByFranquiciaId(franquiciaId).map(mapper::toDomain);
    }
}
