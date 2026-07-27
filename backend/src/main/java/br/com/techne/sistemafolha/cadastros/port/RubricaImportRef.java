package br.com.techne.sistemafolha.cadastros.port;

public record RubricaImportRef(
    Long id,
    String codigo,
    String tipoRubricaDescricao
) {}
