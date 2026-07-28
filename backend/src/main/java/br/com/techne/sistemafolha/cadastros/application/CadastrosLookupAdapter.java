package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRubricaFixaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.LinhaNegocioRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CadastrosLookupAdapter implements CadastrosLookupPort {

    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;
    private final RubricaRepository rubricaRepository;
    private final FuncionarioRubricaFixaRepository funcionarioRubricaFixaRepository;

    @Override
    public Optional<CentroCusto> findCentroCustoById(Long id) {
        return centroCustoRepository.findById(id);
    }

    @Override
    public Optional<LinhaNegocio> findLinhaNegocioById(Long id) {
        return linhaNegocioRepository.findById(id);
    }

    @Override
    public Optional<Rubrica> findRubricaAtivaByCodigo(String codigo) {
        return rubricaRepository.findByCodigo(codigo)
            .filter(r -> Boolean.TRUE.equals(r.getAtivo()));
    }

    @Override
    public List<FuncionarioRubricaFixa> findRubricasFixasVigentesNaCompetencia(
            LocalDate competenciaInicio, LocalDate competenciaFim) {
        return funcionarioRubricaFixaRepository.findVigentesNaCompetencia(competenciaInicio, competenciaFim);
    }
}
