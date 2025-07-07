package br.com.techne.sistemafolha.dto;

import br.com.techne.sistemafolha.model.FolhaPagamento;
import java.util.List;

public record ImportacaoFolhaAdpResponseDTO(
    boolean success,
    String message,
    String arquivo,
    Long tamanho,
    int registrosProcessados,
    List<FolhaPagamentoDTO> folhasPagamento
) {
    public static ImportacaoFolhaAdpResponseDTO success(String arquivo, Long tamanho, List<FolhaPagamento> folhasPagamento) {
        List<FolhaPagamentoDTO> folhasDTO = folhasPagamento.stream()
            .map(folha -> new FolhaPagamentoDTO(
                folha.getId(),
                folha.getFuncionario().getId(),
                folha.getFuncionario().getNome(),
                folha.getRubrica().getId(),
                folha.getRubrica().getCodigo(),
                folha.getRubrica().getDescricao(),
                folha.getRubrica().getTipoRubrica().getDescricao(),
                folha.getCargo() != null ? folha.getCargo().getId() : null,
                folha.getCargo() != null ? folha.getCargo().getDescricao() : null,
                folha.getCentroCusto() != null ? folha.getCentroCusto().getId() : null,
                folha.getCentroCusto() != null ? folha.getCentroCusto().getDescricao() : null,
                folha.getLinhaNegocio() != null ? folha.getLinhaNegocio().getId() : null,
                folha.getLinhaNegocio() != null ? folha.getLinhaNegocio().getDescricao() : null,
                folha.getDataInicio(),
                folha.getDataFim(),
                folha.getValor(),
                folha.getQuantidade(),
                folha.getBaseCalculo()
            ))
            .toList();
            
        return new ImportacaoFolhaAdpResponseDTO(
            true,
            "Arquivo ADP importado com sucesso",
            arquivo,
            tamanho,
            folhasPagamento.size(),
            folhasDTO
        );
    }

    public static ImportacaoFolhaAdpResponseDTO error(String message, String arquivo) {
        return new ImportacaoFolhaAdpResponseDTO(
            false,
            message,
            arquivo,
            0L,
            0,
            List.of()
        );
    }
} 