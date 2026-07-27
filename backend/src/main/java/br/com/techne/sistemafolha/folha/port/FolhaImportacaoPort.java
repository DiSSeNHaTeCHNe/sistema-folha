package br.com.techne.sistemafolha.folha.port;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;

import java.util.List;

public interface FolhaImportacaoPort {

    List<FolhaPagamentoDTO> persistirImportacao(FolhaImportacaoCommand command);
}
