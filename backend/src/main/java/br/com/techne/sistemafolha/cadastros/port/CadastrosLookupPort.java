package br.com.techne.sistemafolha.cadastros.port;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;

import java.util.Optional;

public interface CadastrosLookupPort {

    Optional<CentroCusto> findCentroCustoById(Long id);

    Optional<LinhaNegocio> findLinhaNegocioById(Long id);
}
