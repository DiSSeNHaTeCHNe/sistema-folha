package br.com.techne.sistemafolha.importacao.api;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import java.util.List;

public record ImportacaoFolhaAdpResponseDTO(
    boolean success,
    String message,
    String arquivo,
    Long tamanho,
    int registrosProcessados,
    List<FolhaPagamentoDTO> folhasPagamento,
    boolean conflict,
    String competenciaInicio,
    String competenciaFim,
    Boolean decimoTerceiro
) {
    public static ImportacaoFolhaAdpResponseDTO success(
            String arquivo, Long tamanho, List<FolhaPagamentoDTO> folhasPagamento) {
        return new ImportacaoFolhaAdpResponseDTO(
            true,
            "Arquivo ADP importado com sucesso",
            arquivo,
            tamanho,
            folhasPagamento.size(),
            folhasPagamento,
            false,
            null,
            null,
            null
        );
    }

    public static ImportacaoFolhaAdpResponseDTO error(String message, String arquivo) {
        return new ImportacaoFolhaAdpResponseDTO(
            false,
            message,
            arquivo,
            0L,
            0,
            List.of(),
            false,
            null,
            null,
            null
        );
    }

    public static ImportacaoFolhaAdpResponseDTO conflict(
            String message, String arquivo,
            String competenciaInicio, String competenciaFim, Boolean decimoTerceiro) {
        return new ImportacaoFolhaAdpResponseDTO(
            false,
            message,
            arquivo,
            0L,
            0,
            List.of(),
            true,
            competenciaInicio,
            competenciaFim,
            decimoTerceiro
        );
    }
}
