package it.marconi.biblioteca.controllers.exception;

import it.marconi.biblioteca.domain.response.APIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Errore non gestito: {}", ex.getMessage(), ex);

        return ResponseEntity.internalServerError().body(
                APIResponse.error("Errore interno del server", 500)
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<APIResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Errore gestione risposte: {}", ex.getReason());
        log.trace("Stack trace completo: ", ex);

        int statusCode = ex.getStatusCode().value();
        APIResponse<Void> response = ex.getStatusCode().is5xxServerError()
                ? APIResponse.error("Errore interno del server", statusCode)
                : APIResponse.fail(resolveMessage(ex), statusCode);

        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.putIfAbsent(error.getField(), error.getDefaultMessage())
                );

        return ResponseEntity.badRequest().body(
                APIResponse.fail(
                        errors,
                        "Errore nella validazione dei dati",
                        400
                )
        );
    }

    private String resolveMessage(ResponseStatusException ex) {
        return ex.getReason() != null ? ex.getReason() : "Richiesta non valida";
    }
}
