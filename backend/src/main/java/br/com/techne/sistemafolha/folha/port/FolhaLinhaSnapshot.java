package br.com.techne.sistemafolha.folha.port;

import br.com.techne.sistemafolha.folha.domain.OrigemLinha;

import java.math.BigDecimal;

public record FolhaLinhaSnapshot(
    Long funcionarioId,
    String funcionarioNome,
    Long centroCustoId,
    String centroCustoDescricao,
    Long linhaNegocioId,
    String linhaNegocioDescricao,
    Long cargoId,
    String cargoDescricao,
    Long rubricaId,
    String rubricaCodigo,
    String rubricaDescricao,
    String tipoRubricaDescricao,
    BigDecimal valor,
    short operadorBruto,
    short operadorLiquido,
    short operadorCusto,
    OrigemLinha origemLinha,
    BigDecimal porcentagem
) {}
