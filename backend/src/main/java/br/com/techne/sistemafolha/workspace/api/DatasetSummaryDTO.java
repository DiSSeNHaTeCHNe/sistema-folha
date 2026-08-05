package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Resumo do dataset")
public record DatasetSummaryDTO(
    Long id,
    String nome,
    int schemaVersion,
    long totalLinhas,
    int totalCampos,
    LocalDateTime dataAtualizacao,
    boolean publicado,
    Integer templateVersaoPublicada
) {}
