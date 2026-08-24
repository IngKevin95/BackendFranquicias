package com.franquicias.domain.port;

import com.franquicias.domain.model.Franquicia;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FranquiciaRepositoryPort {
    Mono<Franquicia> save(Franquicia franquicia);
    Mono<Franquicia> findById(UUID id);
    Mono<Void> updateNombre(UUID id, String nombre);
    Flux<Franquicia> findAll();
}
