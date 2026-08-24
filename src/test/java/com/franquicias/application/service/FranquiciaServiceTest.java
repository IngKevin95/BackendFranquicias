package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
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
class FranquiciaServiceTest {

    @Mock
    private FranquiciaRepositoryPort port;

    @InjectMocks
    private FranquiciaService service;

    @Test
    void creaUnaFranquiciaNueva() {
        UUID id = UUID.randomUUID();
        when(port.save(any())).thenReturn(Mono.just(new Franquicia(id, "Frutería Don Pepe")));

        StepVerifier.create(service.crear("Frutería Don Pepe"))
            .expectNextMatches(f -> f.id().equals(id) && f.nombre().equals("Frutería Don Pepe"))
            .verifyComplete();

        verify(port).save(new Franquicia(null, "Frutería Don Pepe"));
    }

    @Test
    void renombraUnaFranquiciaExistente() {
        UUID id = UUID.randomUUID();
        when(port.findById(id)).thenReturn(Mono.just(new Franquicia(id, "Antiguo Nombre")));
        when(port.updateNombre(id, "Nuevo Nombre")).thenReturn(Mono.empty());
        when(port.findById(id)).thenReturn(Mono.just(new Franquicia(id, "Nuevo Nombre")));

        StepVerifier.create(service.renombrar(id, "Nuevo Nombre"))
            .expectNextMatches(f -> f.nombre().equals("Nuevo Nombre"))
            .verifyComplete();
    }

    @Test
    void falloAlRenombrarUnaFranquiciaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(port.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.renombrar(id, "Nombre nuevo"))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }

    @Test
    void listaTodasLasFranquicias() {
        Franquicia f1 = new Franquicia(UUID.randomUUID(), "Frutería Don Pepe");
        Franquicia f2 = new Franquicia(UUID.randomUUID(), "Panadería La Espiga");
        when(port.findAll()).thenReturn(Flux.just(f1, f2));

        StepVerifier.create(service.listar())
            .expectNext(f1)
            .expectNext(f2)
            .verifyComplete();
    }

    @Test
    void obtieneUnaFranquiciaExistente() {
        UUID id = UUID.randomUUID();
        when(port.findById(id)).thenReturn(Mono.just(new Franquicia(id, "Frutería Don Pepe")));

        StepVerifier.create(service.obtener(id))
            .expectNextMatches(f -> f.id().equals(id))
            .verifyComplete();
    }

    @Test
    void falloAlObtenerUnaFranquiciaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(port.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtener(id))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }
}
