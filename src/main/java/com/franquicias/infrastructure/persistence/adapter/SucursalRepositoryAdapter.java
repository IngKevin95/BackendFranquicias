package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.SucursalRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.SucursalMapper;
import com.franquicias.infrastructure.persistence.repository.SucursalR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SucursalRepositoryAdapter implements SucursalRepositoryPort {

    private final SucursalR2dbcRepository repository;
    private final SucursalMapper mapper;

    public SucursalRepositoryAdapter(SucursalR2dbcRepository repository, SucursalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Sucursal> save(Sucursal sucursal) {
        return repository.save(mapper.toEntity(sucursal)).map(mapper::toDomain);
    }

    @Override
    public Mono<Sucursal> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
