package br.com.techne.sistemafolha.exception;

import java.util.List;

public record ValidationErrorResponse(
    int status,
    String message,
    List<FieldErrorItem> errors
) {}
