package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO que representa as informações de controle de acesso do usuário
 * baseadas na sua posição no organograma.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcessoUsuarioDTO {

    private boolean temFuncionarioVinculado;

    private boolean temNoOrganograma;

    private boolean acessoTotal;

    private Set<Long> centrosCustoIds;

    private MotivoNegacaoAcesso motivoNegacao;

    private Long noOrganogramaId;

    private String noOrganogramaNome;

    private Integer nivel;

    private Integer quantidadeCentrosAcessiveis;
}
