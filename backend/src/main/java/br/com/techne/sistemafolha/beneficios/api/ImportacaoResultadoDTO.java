package br.com.techne.sistemafolha.beneficios.api;

import java.math.BigDecimal;
import java.util.List;

public record ImportacaoResultadoDTO(
    int processadas,
    int erros,
    BigDecimal totalValor,
    List<String> detalhesErros
) {}
