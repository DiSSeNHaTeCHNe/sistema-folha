package br.com.techne.sistemafolha.dto;

import br.com.techne.sistemafolha.model.Rubrica.TipoRubrica;

public record RubricaDTO(
    Long id,
    String codigo,
    String descricao,
    TipoRubrica tipo,
    Double porcentagem
) {} 