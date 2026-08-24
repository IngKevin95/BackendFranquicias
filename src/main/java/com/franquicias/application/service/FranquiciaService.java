package com.franquicias.application.service;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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
            .flatMap(existing -> port.updateNombre(id, nuevoNombre).then(port.findById(id)));
    }

    public Flux<Franquicia> listar() {
        return port.findAll();
    }

    public Mono<Franquicia> obtener(UUID id) {
        return port.findById(id)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(id)));
    }
}
