package br.com.techne.sistemafolha.workspace.domain;

public record DatasetFieldSchema(
    String nome,
    DatasetFieldType tipo,
    ReferenciaEntidade referenciaEntidade,
    Boolean obrigatorio
) {}
