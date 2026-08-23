package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.ProductoNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.ProductoRepositoryPort;
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
class ProductoServiceTest {

    @Mock
    private SucursalRepositoryPort sucursalPort;
    @Mock
    private ProductoRepositoryPort productoPort;
    @Mock
    private FranquiciaRepositoryPort franquiciaPort;

    @InjectMocks
    private ProductoService service;

    private static final UUID FRANQUICIA_ID = UUID.randomUUID();
    private static final UUID SUCURSAL_ID = UUID.randomUUID();

    @Test
    void agregaUnProductoAUnaSucursalExistente() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.save(any()))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));

        StepVerifier.create(service.agregar(FRANQUICIA_ID, SUCURSAL_ID, "Manzana", 10))
            .expectNextMatches(p -> p.nombre().equals("Manzana") && p.stock() == 10)
            .verifyComplete();
    }

    @Test
    void falloAlAgregarProductoASucursalDeOtraFranquicia() {
        UUID otraFranquiciaId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, otraFranquiciaId, "Sede Norte")));

        StepVerifier.create(service.agregar(FRANQUICIA_ID, SUCURSAL_ID, "Manzana", 10))
            .expectError(SucursalNotFoundException.class)
            .verify();
    }

    @Test
    void eliminaUnProductoExistente() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        when(productoPort.deleteById(productoId)).thenReturn(Mono.empty());

        StepVerifier.create(service.eliminar(FRANQUICIA_ID, SUCURSAL_ID, productoId))
            .verifyComplete();
    }

    @Test
    void modificaElStockDeUnProducto() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        when(productoPort.save(any()))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 50)));

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, 50))
            .expectNextMatches(p -> p.stock() == 50)
            .verifyComplete();
    }

    @Test
    void falloAlModificarStockDeProductoDeOtraSucursal() {
        UUID productoId = UUID.randomUUID();
        UUID otraSucursalId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, otraSucursalId, "Manzana", 10)));

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, 50))
            .expectError(ProductoNotFoundException.class)
            .verify();
    }

    @Test
    void obtieneElProductoConMasStockPorSucursal() {
        when(franquiciaPort.findById(FRANQUICIA_ID))
            .thenReturn(Mono.just(new Franquicia(FRANQUICIA_ID, "Frutería")));
        ProductoMaxStock resultado = new ProductoMaxStock(
            SUCURSAL_ID, "Sede Norte", new Producto(UUID.randomUUID(), SUCURSAL_ID, "Pera", 25));
        when(productoPort.findMaxStockPorFranquicia(FRANQUICIA_ID)).thenReturn(Flux.just(resultado));

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID))
            .expectNext(resultado)
            .verifyComplete();
    }

    @Test
    void falloAlObtenerMaxStockDeFranquiciaInexistente() {
        when(franquiciaPort.findById(FRANQUICIA_ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }
}
