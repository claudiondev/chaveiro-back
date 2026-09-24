package com.chaveiro_abencoado.back.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Erro de validação");

        return buildResponse(mensagem, HttpStatus.BAD_REQUEST);
    }

    // Parâmetros de query/path inválidos (ex: page=-1, size=100)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> tratarParametroInvalido(ConstraintViolationException ex) {
        String mensagem = ex.getConstraintViolations().stream()
                .map(v -> {
                    String caminho = v.getPropertyPath().toString();
                    return caminho.substring(caminho.lastIndexOf('.') + 1) + ": " + v.getMessage();
                })
                .findFirst()
                .orElse("Parâmetro inválido");

        return buildResponse(mensagem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return buildResponse(ex.getName() + ": valor inválido", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return buildResponse("Corpo da requisição inválido", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> tratarNotFound(NotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> tratarBusiness(BusinessException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> tratarUnauthorized(UnauthorizedException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> tratarRuntime(RuntimeException ex) {
        log.warn("RuntimeException não tratada: ", ex);
        return buildResponse("Erro inesperado na operação", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> tratarErroInesperado(Exception ex) {
        log.error("Erro inesperado: ", ex);
        return buildResponse("Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String mensagem, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("erro", mensagem);
        body.put("status", status.value());
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
