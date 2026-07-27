package br.com.techne.sistemafolha.organograma.acesso.port;

import java.util.Set;

public interface OrganogramaAcessoPort {

    Set<Long> obterCentrosCustoAcessiveis(Long usuarioId);

    boolean usuarioPodeAcessarCentroCusto(Long usuarioId, Long centroCustoId);

    AccessContextDTO obterContextoAcesso(Long usuarioId);
}
