package br.com.techne.sistemafolha.dto;

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
    
    /**
     * ID do nó do organograma onde o usuário está posicionado.
     * Null se o usuário não tem funcionário ou não está em nenhum nó.
     */
    private Long noOrganogramaId;
    
    /**
     * Nome do nó do organograma.
     */
    private String noOrganogramaNome;
    
    /**
     * Nível hierárquico do nó (0 = raiz).
     */
    private Integer nivel;
    
    /**
     * IDs dos centros de custo que o usuário pode acessar.
     * Set vazio significa acesso total (sem restrições).
     */
    private Set<Long> centrosCustoAcessiveis;
    
    /**
     * Indica se o usuário tem acesso total sem restrições.
     * True quando não há funcionário vinculado ou funcionário não está em nenhum nó.
     */
    private Boolean acessoTotal;
    
    /**
     * Quantidade de centros de custo acessíveis.
     */
    private Integer quantidadeCentrosAcessiveis;
}

