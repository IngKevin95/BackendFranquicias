package com.franquicias.domain.port;

import com.franquicias.domain.model.Sucursal;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SucursalRepositoryPort {
    Mono<Sucursal> save(Sucursal sucursal);
    Mono<Sucursal> findById(UUID id);
    Mono<Void> updateNombre(UUID id, String nombre);
    Flux<Sucursal> findByFranquiciaId(UUID franquiciaId);
}
