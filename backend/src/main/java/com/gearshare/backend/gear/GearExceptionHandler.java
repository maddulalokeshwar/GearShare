
package com.gearshare.backend.gear;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GearExceptionHandler {

    @ExceptionHandler(GearNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(
            GearNotFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidInput(
            IllegalArgumentException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(
            HttpMessageNotReadableException exception) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error",
                        "Invalid or incomplete JSON request body"));
    }
}