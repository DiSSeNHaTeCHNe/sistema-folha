package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;

public record EvolucaoMensalDTO(
    String mesAno,
    BigDecimal valorTotal,
    Integer quantidadeFuncionarios
) {} 