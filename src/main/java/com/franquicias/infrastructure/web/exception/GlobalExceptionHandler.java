package com.franquicias.infrastructure.web.exception;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.ProductoNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler({FranquiciaNotFoundException.class, SucursalNotFoundException.class, ProductoNotFoundException.class})
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(RuntimeException ex, ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Solicitud inválida");
        return build(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadInput(ServerWebInputException ex, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, ex.getReason(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex, ServerWebExchange exchange) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno inesperado", exchange);
    }

    private Mono<ResponseEntity<ErrorResponse>> build(HttpStatus status, String message, ServerWebExchange exchange) {
        ErrorResponse body = ErrorResponse.of(
            status.value(), status.getReasonPhrase(), message, exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(status).body(body));
    }
}
