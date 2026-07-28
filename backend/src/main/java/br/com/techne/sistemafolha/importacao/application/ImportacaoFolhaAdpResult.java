package br.com.techne.sistemafolha.importacao.application;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;

import java.util.List;

public record ImportacaoFolhaAdpResult(
    List<FolhaPagamentoDTO> folhasPagamento,
    ProcessamentoResultadoDTO processamento
) {}
