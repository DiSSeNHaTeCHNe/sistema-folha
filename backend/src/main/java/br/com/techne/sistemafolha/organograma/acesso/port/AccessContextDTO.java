package br.com.techne.sistemafolha.organograma.acesso.port;

import java.util.Set;

public record AccessContextDTO(
    boolean temFuncionarioVinculado,
    boolean temNoOrganograma,
    boolean acessoTotal,
    Set<Long> centrosCustoIds,
    MotivoNegacaoAcesso motivoNegacao,
    Long noOrganogramaId,
    String noOrganogramaNome,
    Integer nivel
) {}
