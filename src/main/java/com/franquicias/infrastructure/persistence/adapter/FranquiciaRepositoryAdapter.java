package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.FranquiciaMapper;
import com.franquicias.infrastructure.persistence.repository.FranquiciaR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FranquiciaRepositoryAdapter implements FranquiciaRepositoryPort {

    private final FranquiciaR2dbcRepository repository;
    private final FranquiciaMapper mapper;

    public FranquiciaRepositoryAdapter(FranquiciaR2dbcRepository repository, FranquiciaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Franquicia> save(Franquicia franquicia) {
        return repository.save(mapper.toEntity(franquicia)).map(mapper::toDomain);
    }

    @Override
    public Mono<Franquicia> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
