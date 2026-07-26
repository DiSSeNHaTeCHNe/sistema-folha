package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.BeneficioMensalDTO;
import br.com.techne.sistemafolha.dto.BeneficioMensalResumoDTO;
import br.com.techne.sistemafolha.exception.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.exception.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.exception.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.model.BeneficioMensal;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.TipoBeneficio;
import br.com.techne.sistemafolha.repository.BeneficioMensalRepository;
import br.com.techne.sistemafolha.repository.BeneficioMensalResumoProjection;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import br.com.techne.sistemafolha.repository.TipoBeneficioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficioMensalService {

    private final BeneficioMensalRepository beneficioMensalRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final TipoBeneficioRepository tipoBeneficioRepository;

    public List<BeneficioMensalDTO> listarPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        return buscarPorCompetencia(dataInicio, dataFim, centros).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<BeneficioMensalResumoDTO> resumoPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        return buscarResumoPorCompetencia(dataInicio, dataFim, centros).stream()
                .map(this::toResumoDTO)
                .collect(Collectors.toList());
    }

    public List<BeneficioMensalDTO> listarPorFuncionario(
            Long funcionarioId, LocalDate dataInicio, LocalDate dataFim) {
        return beneficioMensalRepository
                .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                        funcionarioId, dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BeneficioMensalDTO criar(BeneficioMensalDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));

        TipoBeneficio tipoBeneficio = tipoBeneficioRepository.findById(dto.tipoBeneficioId())
                .filter(t -> Boolean.TRUE.equals(t.getAtivo()))
                .orElseThrow(() -> new TipoBeneficioNotFoundException(dto.tipoBeneficioId()));

        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setFuncionario(funcionario);
        beneficio.setTipoBeneficio(tipoBeneficio);
        beneficio.setValor(dto.valor());
        beneficio.setCompetenciaInicio(dto.competenciaInicio());
        beneficio.setCompetenciaFim(dto.competenciaFim());
        beneficio.setObservacao(dto.observacao());
        beneficio.setAtivo(true);

        return toDTO(beneficioMensalRepository.save(beneficio));
    }

    @Transactional
    public void remover(Long id) {
        BeneficioMensal beneficio = beneficioMensalRepository.findById(id)
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .orElseThrow(() -> new BeneficioMensalNotFoundException(id));
        beneficio.setAtivo(false);
        beneficioMensalRepository.save(beneficio);
    }

    private List<BeneficioMensal> buscarPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        if (centros.isEmpty()) {
            return beneficioMensalRepository
                    .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(dataInicio, dataFim);
        }
        return beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue(
                        dataInicio, dataFim, centros);
    }

    private List<BeneficioMensalResumoProjection> buscarResumoPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        if (centros.isEmpty()) {
            return beneficioMensalRepository.resumoPorCompetencia(dataInicio, dataFim);
        }
        return beneficioMensalRepository.resumoPorCompetenciaAndCentroCustoIds(
                dataInicio, dataFim, centros);
    }

    private BeneficioMensalDTO toDTO(BeneficioMensal beneficio) {
        Funcionario funcionario = beneficio.getFuncionario();
        TipoBeneficio tipo = beneficio.getTipoBeneficio();

        Long centroCustoId = null;
        String centroCustoDescricao = null;
        if (funcionario != null && funcionario.getCentroCusto() != null) {
            centroCustoId = funcionario.getCentroCusto().getId();
            centroCustoDescricao = funcionario.getCentroCusto().getDescricao();
        }

        return new BeneficioMensalDTO(
                beneficio.getId(),
                funcionario != null ? funcionario.getId() : null,
                funcionario != null ? funcionario.getNome() : null,
                tipo != null ? tipo.getId() : null,
                tipo != null ? tipo.getCodigo() : null,
                tipo != null ? tipo.getDescricao() : null,
                centroCustoId,
                centroCustoDescricao,
                beneficio.getValor(),
                beneficio.getCompetenciaInicio(),
                beneficio.getCompetenciaFim(),
                beneficio.getObservacao()
        );
    }

    private BeneficioMensalResumoDTO toResumoDTO(BeneficioMensalResumoProjection projection) {
        return new BeneficioMensalResumoDTO(
                projection.getCodigo(),
                projection.getDescricao(),
                projection.getTotal(),
                projection.getQtdLancamentos()
        );
    }
}
