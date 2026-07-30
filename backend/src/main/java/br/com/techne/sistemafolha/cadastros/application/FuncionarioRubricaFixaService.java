package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.FuncionarioRubricaFixaDTO;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaVigenciaConflictException;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRubricaFixaRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuncionarioRubricaFixaService {

    private static final Set<String> RUBRICAS_CALCULADAS = Set.of("5000");

    private final FuncionarioRubricaFixaRepository funcionarioRubricaFixaRepository;
    private final FuncionarioConsultaPort funcionarioConsultaPort;
    private final RubricaRepository rubricaRepository;

    @Transactional(readOnly = true)
    public List<FuncionarioRubricaFixaDTO> listar(Long funcionarioId, Long rubricaId) {
        return funcionarioRubricaFixaRepository.findByFiltros(funcionarioId, rubricaId).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioRubricaFixaDTO buscarPorId(Long id) {
        FuncionarioRubricaFixa entity = funcionarioRubricaFixaRepository.findById(id)
            .filter(e -> Boolean.TRUE.equals(e.getAtivo()))
            .orElseThrow(() -> new FuncionarioRubricaFixaNotFoundException(id));
        return toDTO(entity);
    }

    @Transactional
    public FuncionarioRubricaFixaDTO criar(FuncionarioRubricaFixaDTO dto) {
        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(dto.rubricaId())
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada: " + dto.rubricaId()));

        validarValor(dto.valor(), rubrica);
        validarVigencia(dto.vigenciaInicio(), dto.vigenciaFim());

        Funcionario funcionario = null;
        if (dto.funcionarioId() != null) {
            funcionario = funcionarioConsultaPort.findByIdAndAtivoTrue(dto.funcionarioId())
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
            validarSobreposicaoIndividual(
                funcionario.getId(), rubrica.getId(), dto.vigenciaInicio(), dto.vigenciaFim(), null);
        } else {
            validarSobreposicaoGlobal(rubrica.getId(), dto.vigenciaInicio(), dto.vigenciaFim(), null);
        }

        FuncionarioRubricaFixa entity = new FuncionarioRubricaFixa();
        entity.setFuncionario(funcionario);
        entity.setRubrica(rubrica);
        entity.setValor(dto.valor());
        entity.setVigenciaInicio(dto.vigenciaInicio());
        entity.setVigenciaFim(dto.vigenciaFim());
        entity.setComentario(dto.comentario());
        entity.setAtivo(true);

        return toDTO(funcionarioRubricaFixaRepository.save(entity));
    }

    @Transactional
    public FuncionarioRubricaFixaDTO atualizar(Long id, FuncionarioRubricaFixaDTO dto) {
        FuncionarioRubricaFixa entity = funcionarioRubricaFixaRepository.findById(id)
            .filter(e -> Boolean.TRUE.equals(e.getAtivo()))
            .orElseThrow(() -> new FuncionarioRubricaFixaNotFoundException(id));

        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(dto.rubricaId())
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada: " + dto.rubricaId()));

        validarValor(dto.valor(), rubrica);
        validarVigencia(dto.vigenciaInicio(), dto.vigenciaFim());

        Funcionario funcionario = null;
        if (dto.funcionarioId() != null) {
            funcionario = funcionarioConsultaPort.findByIdAndAtivoTrue(dto.funcionarioId())
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
            validarSobreposicaoIndividual(
                funcionario.getId(), rubrica.getId(), dto.vigenciaInicio(), dto.vigenciaFim(), id);
        } else {
            validarSobreposicaoGlobal(rubrica.getId(), dto.vigenciaInicio(), dto.vigenciaFim(), id);
        }

        entity.setFuncionario(funcionario);
        entity.setRubrica(rubrica);
        entity.setValor(dto.valor());
        entity.setVigenciaInicio(dto.vigenciaInicio());
        entity.setVigenciaFim(dto.vigenciaFim());
        entity.setComentario(dto.comentario());

        return toDTO(funcionarioRubricaFixaRepository.save(entity));
    }

    @Transactional
    public void remover(Long id) {
        FuncionarioRubricaFixa entity = funcionarioRubricaFixaRepository.findById(id)
            .filter(e -> Boolean.TRUE.equals(e.getAtivo()))
            .orElseThrow(() -> new FuncionarioRubricaFixaNotFoundException(id));
        entity.setAtivo(false);
        funcionarioRubricaFixaRepository.save(entity);
    }

    private void validarValor(BigDecimal valor, Rubrica rubrica) {
        if (valor == null && !isRubricaCalculada(rubrica)) {
            throw new IllegalArgumentException("Valor é obrigatório para rubricas não calculadas");
        }
    }

    private void validarVigencia(LocalDate inicio, LocalDate fim) {
        if (fim != null && fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Vigência fim não pode ser anterior ao início");
        }
    }

    private void validarSobreposicaoIndividual(
            Long funcionarioId, Long rubricaId,
            LocalDate vigenciaInicio, LocalDate vigenciaFim, Long excludeId) {
        if (funcionarioRubricaFixaRepository.existsVigenciaSobreposta(
                funcionarioId, rubricaId, vigenciaInicio, vigenciaFim, excludeId)) {
            throw FuncionarioRubricaFixaVigenciaConflictException.forIndividual();
        }
    }

    private void validarSobreposicaoGlobal(
            Long rubricaId, LocalDate vigenciaInicio, LocalDate vigenciaFim, Long excludeId) {
        if (funcionarioRubricaFixaRepository.existsVigenciaSobrepostaGlobal(
                rubricaId, vigenciaInicio, vigenciaFim, excludeId)) {
            throw FuncionarioRubricaFixaVigenciaConflictException.forGlobal();
        }
    }

    boolean isRubricaCalculada(Rubrica rubrica) {
        return rubrica.getCodigo() != null && RUBRICAS_CALCULADAS.contains(rubrica.getCodigo());
    }

    private FuncionarioRubricaFixaDTO toDTO(FuncionarioRubricaFixa entity) {
        Funcionario funcionario = entity.getFuncionario();
        return new FuncionarioRubricaFixaDTO(
            entity.getId(),
            funcionario != null ? funcionario.getId() : null,
            entity.getRubrica().getId(),
            entity.getValor(),
            entity.getVigenciaInicio(),
            entity.getVigenciaFim(),
            entity.getComentario(),
            entity.getAtivo(),
            funcionario != null ? funcionario.getNome() : null,
            entity.getRubrica().getCodigo(),
            entity.getRubrica().getDescricao(),
            entity.getRubrica().getPorcentagem()
        );
    }
}
