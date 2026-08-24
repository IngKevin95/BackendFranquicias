package com.franquicias.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Sucursal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SucursalRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private FranquiciaRepositoryAdapter franquiciaAdapter;
    @Autowired
    private SucursalRepositoryAdapter sucursalAdapter;

    @Test
    void guardaYRecuperaUnaSucursalAsociadaAUnaFranquicia() {
        Franquicia franquicia = franquiciaAdapter.save(new Franquicia(null, "Frutería Don Pepe")).block();

        Sucursal guardada = sucursalAdapter.save(new Sucursal(null, franquicia.id(), "Sede Norte")).block();

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada.franquiciaId()).isEqualTo(franquicia.id());

        Sucursal encontrada = sucursalAdapter.findById(guardada.id()).block();
        assertThat(encontrada.nombre()).isEqualTo("Sede Norte");
    }
}
