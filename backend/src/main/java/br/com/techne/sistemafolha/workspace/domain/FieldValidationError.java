package br.com.techne.sistemafolha.workspace.domain;

public record FieldValidationError(
    String field,
    String message
) {}
