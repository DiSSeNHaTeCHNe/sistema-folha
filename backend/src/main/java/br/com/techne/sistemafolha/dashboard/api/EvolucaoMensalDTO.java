package br.com.techne.sistemafolha.dashboard.api;

import java.math.BigDecimal;

public record EvolucaoMensalDTO(
    String mesAno,
    BigDecimal valorTotal,
    Integer quantidadeFuncionarios
) {} 