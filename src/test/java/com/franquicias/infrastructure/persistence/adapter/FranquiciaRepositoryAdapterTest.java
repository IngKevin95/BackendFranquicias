package com.franquicias.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.Franquicia;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
class FranquiciaRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private FranquiciaRepositoryAdapter adapter;

    @Test
    void guardaYRecuperaUnaFranquicia() {
        Franquicia guardada = adapter.save(new Franquicia(null, "Frutería Don Pepe")).block();

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada.nombre()).isEqualTo("Frutería Don Pepe");

        Franquicia encontrada = adapter.findById(guardada.id()).block();
        assertThat(encontrada.nombre()).isEqualTo("Frutería Don Pepe");
    }
}
