package com.procurement.supplier.exception;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap; // Store field names and error messages
import org.springframework.web.bind.MethodArgumentNotValidException;

@RestControllerAdvice // Handle exceptions across all REST controllers
public class GlobalExceptionHandler {

    @ExceptionHandler(SupplierNotFoundException.class) // Catch this custom exception
    public ResponseEntity<Map<String, String>> handleSupplierNotFound(
            SupplierNotFoundException exception) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND) // Return HTTP 404
                .body(Collections.singletonMap("message", exception.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new LinkedHashMap<>(); // Collect validation errors

        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())); // Map field to message

        return ResponseEntity.badRequest().body(errors); // Return HTTP 400
    }
    
    @ExceptionHandler(DuplicateSupplierException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateSupplier(
            DuplicateSupplierException exception) {

        // Return HTTP 409 with a clear error message
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Collections.singletonMap("message", exception.getMessage()));
    }
}