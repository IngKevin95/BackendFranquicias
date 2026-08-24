package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.SucursalRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    @Mock
    private FranquiciaRepositoryPort franquiciaPort;
    @Mock
    private SucursalRepositoryPort sucursalPort;

    @InjectMocks
    private SucursalService service;

    @Test
    void agregaUnaSucursalAUnaFranquiciaExistente() {
        UUID franquiciaId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(franquiciaPort.findById(franquiciaId)).thenReturn(Mono.just(new Franquicia(franquiciaId, "Frutería")));
        when(sucursalPort.save(any())).thenReturn(Mono.just(new Sucursal(sucursalId, franquiciaId, "Sede Norte")));

        StepVerifier.create(service.agregar(franquiciaId, "Sede Norte"))
            .expectNextMatches(s -> s.id().equals(sucursalId) && s.franquiciaId().equals(franquiciaId))
            .verifyComplete();
    }

    @Test
    void falloAlAgregarSucursalAFranquiciaInexistente() {
        UUID franquiciaId = UUID.randomUUID();
        when(franquiciaPort.findById(franquiciaId)).thenReturn(Mono.empty());

        StepVerifier.create(service.agregar(franquiciaId, "Sede Norte"))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }

    @Test
    void falloAlRenombrarSucursalQuePerteneceAOtraFranquicia() {
        UUID franquiciaId = UUID.randomUUID();
        UUID otraFranquiciaId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(sucursalPort.findById(sucursalId))
            .thenReturn(Mono.just(new Sucursal(sucursalId, otraFranquiciaId, "Sede Norte")));

        StepVerifier.create(service.renombrar(franquiciaId, sucursalId, "Nuevo nombre"))
            .expectError(com.franquicias.domain.exception.ConflictoRelacionException.class)
            .verify();
    }

    @Test
    void listaSucursalesDeUnaFranquiciaExistente() {
        UUID franquiciaId = UUID.randomUUID();
        Sucursal s1 = new Sucursal(UUID.randomUUID(), franquiciaId, "Sede Norte");
        Sucursal s2 = new Sucursal(UUID.randomUUID(), franquiciaId, "Sede Sur");
        when(franquiciaPort.findById(franquiciaId)).thenReturn(Mono.just(new Franquicia(franquiciaId, "Frutería")));
        when(sucursalPort.findByFranquiciaId(franquiciaId)).thenReturn(Flux.just(s1, s2));

        StepVerifier.create(service.listarPorFranquicia(franquiciaId))
            .expectNext(s1)
            .expectNext(s2)
            .verifyComplete();
    }

    @Test
    void falloAlListarSucursalesDeFranquiciaInexistente() {
        UUID franquiciaId = UUID.randomUUID();
        when(franquiciaPort.findById(franquiciaId)).thenReturn(Mono.empty());

        StepVerifier.create(service.listarPorFranquicia(franquiciaId))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }
}
