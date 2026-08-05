package br.com.techne.sistemafolha.exception;

public record FieldErrorItem(
    String field,
    String message
) {}
