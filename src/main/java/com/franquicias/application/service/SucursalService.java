package com.franquicias.application.service;

import com.franquicias.domain.exception.ConflictoRelacionException;
import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.SucursalRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SucursalService {

    private final FranquiciaRepositoryPort franquiciaPort;
    private final SucursalRepositoryPort sucursalPort;

    public SucursalService(FranquiciaRepositoryPort franquiciaPort, SucursalRepositoryPort sucursalPort) {
        this.franquiciaPort = franquiciaPort;
        this.sucursalPort = sucursalPort;
    }

    public Mono<Sucursal> agregar(UUID franquiciaId, String nombre) {
        return franquiciaPort.findById(franquiciaId)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(franquiciaId)))
            .flatMap(franquicia -> sucursalPort.save(new Sucursal(null, franquiciaId, nombre)));
    }

    public Mono<Sucursal> renombrar(UUID franquiciaId, UUID sucursalId, String nuevoNombre) {
        return sucursalPort.findById(sucursalId)
            .switchIfEmpty(Mono.error(new SucursalNotFoundException(sucursalId)))
            .flatMap(sucursal -> {
                if (!sucursal.franquiciaId().equals(franquiciaId)) {
                    return Mono.error(new ConflictoRelacionException("La sucursal no pertenece a esta franquicia"));
                }
                return sucursalPort.updateNombre(sucursalId, nuevoNombre).then(sucursalPort.findById(sucursalId));
            });
    }

    public Flux<Sucursal> listarPorFranquicia(UUID franquiciaId) {
        return franquiciaPort.findById(franquiciaId)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(franquiciaId)))
            .flatMapMany(franquicia -> sucursalPort.findByFranquiciaId(franquiciaId));
    }
}
