package br.com.techne.sistemafolha.cadastros.port;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CadastrosLookupPort {

    Optional<CentroCusto> findCentroCustoById(Long id);

    Optional<LinhaNegocio> findLinhaNegocioById(Long id);

    Optional<Rubrica> findRubricaAtivaByCodigo(String codigo);

    List<FuncionarioRubricaFixa> findRubricasFixasVigentesNaCompetencia(
        LocalDate competenciaInicio, LocalDate competenciaFim);
}
