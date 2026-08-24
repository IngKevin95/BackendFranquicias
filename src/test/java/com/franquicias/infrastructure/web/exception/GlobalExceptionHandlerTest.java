package com.franquicias.infrastructure.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapeaFranquiciaNotFoundA404() {
        UUID id = UUID.randomUUID();
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/franquicias/" + id).build());

        Mono<ErrorResponse> result = handler.handleNotFound(new FranquiciaNotFoundException(id), exchange)
            .map(entity -> {
                assertThat(entity.getStatusCode().value()).isEqualTo(404);
                return entity.getBody();
            });

        StepVerifier.create(result)
            .expectNextMatches(body -> body.status() == 404 && body.error().equals("Not Found"))
            .verifyComplete();
    }

    @Test
    void manejaServerWebInputExceptionConRazonNulo() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/productos").build());
        ServerWebInputException ex = new ServerWebInputException(null, null);

        Mono<ErrorResponse> result = handler.handleBadInput(ex, exchange)
            .map(entity -> {
                assertThat(entity.getStatusCode().value()).isEqualTo(400);
                return entity.getBody();
            });

        StepVerifier.create(result)
            .expectNextMatches(body -> body.status() == 400 && body.message().equals("Solicitud inválida"))
            .verifyComplete();
    }
}
