package com.franquicias.domain.port;

import com.franquicias.domain.model.Usuario;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UsuarioRepositoryPort {
    Mono<Usuario> findByUsername(String username);
    Mono<Usuario> save(Usuario usuario);
    Flux<Usuario> findAll();
}
