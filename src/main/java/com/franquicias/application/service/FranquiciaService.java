package com.franquicias.application.service;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FranquiciaService {

    private final FranquiciaRepositoryPort port;

    public FranquiciaService(FranquiciaRepositoryPort port) {
        this.port = port;
    }

    public Mono<Franquicia> crear(String nombre) {
        return port.save(new Franquicia(null, nombre));
    }

    public Mono<Franquicia> renombrar(UUID id, String nuevoNombre) {
        return port.findById(id)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(id)))
            .flatMap(existing -> port.save(new Franquicia(existing.id(), nuevoNombre)));
    }
}
