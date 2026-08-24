package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.ConflictoRelacionException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
    @Mock
    private ReactiveRedisTemplate<String, Object> redisTemplate;
    @Mock
    private ReactiveListOperations<String, Object> listOperations;

    private ProductoService service;

    private static final UUID FRANQUICIA_ID = UUID.randomUUID();
    private static final UUID SUCURSAL_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductoService(sucursalPort, productoPort, franquiciaPort, redisTemplate);
    }

    private void mockRedisDelete() {
        when(redisTemplate.keys(anyString())).thenReturn(Flux.empty());
    }

    @Test
    void agregaUnProductoAUnaSucursalExistente() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.save(any()))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 0)));
        mockRedisDelete();

        StepVerifier.create(service.agregar(FRANQUICIA_ID, SUCURSAL_ID, "Manzana"))
            .expectNextMatches(p -> p.nombre().equals("Manzana") && p.stock() == 0)
            .verifyComplete();
    }

    @Test
    void falloAlAgregarProductoASucursalDeOtraFranquicia() {
        UUID otraFranquiciaId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, otraFranquiciaId, "Sede Norte")));

        StepVerifier.create(service.agregar(FRANQUICIA_ID, SUCURSAL_ID, "Manzana"))
            .expectError(ConflictoRelacionException.class)
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
        mockRedisDelete();

        StepVerifier.create(service.eliminar(FRANQUICIA_ID, SUCURSAL_ID, productoId))
            .verifyComplete();
    }

    @Test
    void modificaElStockDeUnProductoConEntrada() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        when(productoPort.updateStockNativo(productoId, 50)).thenReturn(Mono.just(1L));
        when(productoPort.registrarTransaccionStock(productoId, "ENTRADA", 50, null)).thenReturn(Mono.empty());
        mockRedisDelete();
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 60)));

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, "ENTRADA", 50, null))
            .expectNextMatches(p -> p.stock() == 60)
            .verifyComplete();
    }
    
    @Test
    void modificaElStockDeUnProductoConSalidaFallida() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        when(productoPort.updateStockNativo(productoId, -50)).thenReturn(Mono.just(0L)); // 0 filas actualizadas significa que el stock no alcanzó

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, "SALIDA", 50, null))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void falloAlModificarStockDeProductoDeOtraSucursal() {
        UUID productoId = UUID.randomUUID();
        UUID otraSucursalId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, otraSucursalId, "Manzana", 10)));

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, "ENTRADA", 50, null))
            .expectError(ConflictoRelacionException.class)
            .verify();
    }

    @Test
    void renombraUnProductoExistente() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        
        when(productoPort.updateNombre(productoId, "Pera")).thenReturn(Mono.empty());
        mockRedisDelete();
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Pera", 10)));

        StepVerifier.create(service.renombrar(FRANQUICIA_ID, SUCURSAL_ID, productoId, "Pera"))
            .expectNextMatches(p -> p.nombre().equals("Pera"))
            .verifyComplete();
    }

    @Test
    void obtieneElProductoConMasStockPorSucursal() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), eq(0L), eq(-1L))).thenReturn(Flux.empty());
        when(franquiciaPort.findById(FRANQUICIA_ID))
            .thenReturn(Mono.just(new Franquicia(FRANQUICIA_ID, "Frutería")));
        ProductoMaxStock resultado = new ProductoMaxStock(
            SUCURSAL_ID, "Sede Norte", new Producto(UUID.randomUUID(), SUCURSAL_ID, "Pera", 25));
        when(productoPort.findMaxStockPorFranquicia(FRANQUICIA_ID, 10, 0)).thenReturn(Flux.just(resultado));
        when(listOperations.rightPushAll(anyString(), (Object[]) any())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID, 10, 0))
            .expectNext(resultado)
            .verifyComplete();
    }

    @Test
    void obtieneElProductoConMasStockDesdeCache() {
        ProductoMaxStock resultado = new ProductoMaxStock(
            SUCURSAL_ID, "Sede Norte", new Producto(UUID.randomUUID(), SUCURSAL_ID, "Pera", 25));
        
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), eq(0L), eq(-1L))).thenReturn(Flux.just(resultado));

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID, 10, 0))
            .expectNext(resultado)
            .verifyComplete();
    }

    @Test
    void falloAlObtenerMaxStockDeFranquiciaInexistente() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), eq(0L), eq(-1L))).thenReturn(Flux.empty());
        when(franquiciaPort.findById(FRANQUICIA_ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID, 10, 0))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }
}
