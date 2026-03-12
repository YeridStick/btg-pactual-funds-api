package co.btg.api.config;

import co.btg.api.dto.ApiResponse;
import co.btg.model.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalErrorHandler {

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleBusinessException(BusinessException ex) {
        log.warn("Regla de negocio no cumplida: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .mensaje(ex.getMessage())
                .data(null)
                .codigo(HttpStatus.BAD_REQUEST.value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleValidationException(WebExchangeBindException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiResponse<Object> response = ApiResponse.builder()
                .mensaje("Datos de entrada inválidos: " + errores)
                .data(null)
                .codigo(HttpStatus.BAD_REQUEST.value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response));
    }

    // Errores (No controlados)
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleGeneralException(Exception ex) {
        log.error("Error interno no controlado", ex);

        ApiResponse<Object> response = ApiResponse.builder()
                .mensaje("Ha ocurrido un error inesperado. Por favor, contacte al soporte.")
                .data(null)
                .codigo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
    }
}