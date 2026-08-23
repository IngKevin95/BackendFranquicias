package com.franquicias.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.model.Sucursal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductoRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private FranquiciaRepositoryAdapter franquiciaAdapter;
    @Autowired
    private SucursalRepositoryAdapter sucursalAdapter;
    @Autowired
    private ProductoRepositoryAdapter productoAdapter;

    @Test
    void encuentraElProductoConMasStockPorCadaSucursalDeLaFranquicia() {
        Franquicia franquicia = franquiciaAdapter.save(new Franquicia(null, "Frutería Don Pepe")).block();
        Sucursal sedeNorte = sucursalAdapter.save(new Sucursal(null, franquicia.id(), "Sede Norte")).block();
        Sucursal sedeSur = sucursalAdapter.save(new Sucursal(null, franquicia.id(), "Sede Sur")).block();

        productoAdapter.save(new Producto(null, sedeNorte.id(), "Manzana", 10)).block();
        productoAdapter.save(new Producto(null, sedeNorte.id(), "Pera", 25)).block();
        productoAdapter.save(new Producto(null, sedeSur.id(), "Uva", 5)).block();

        List<ProductoMaxStock> resultado = productoAdapter.findMaxStockPorFranquicia(franquicia.id())
            .collectList().block();

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
            .filteredOn(r -> r.sucursalId().equals(sedeNorte.id()))
            .extracting(r -> r.producto().nombre())
            .containsExactly("Pera");
        assertThat(resultado)
            .filteredOn(r -> r.sucursalId().equals(sedeSur.id()))
            .extracting(r -> r.producto().nombre())
            .containsExactly("Uva");
    }
}
