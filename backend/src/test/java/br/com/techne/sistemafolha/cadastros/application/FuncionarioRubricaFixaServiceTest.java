package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.FuncionarioRubricaFixaDTO;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaVigenciaConflictException;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRubricaFixaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioRubricaFixaServiceTest {

    private static final Long FUNCIONARIO_ID = 1L;
    private static final Long RUBRICA_ID = 10L;
    private static final LocalDate VIGENCIA_INICIO = LocalDate.of(2024, 1, 1);

    @Mock
    private FuncionarioRubricaFixaRepository funcionarioRubricaFixaRepository;

    @Mock
    private FuncionarioConsultaPort funcionarioConsultaPort;

    @Mock
    private RubricaRepository rubricaRepository;

    @InjectMocks
    private FuncionarioRubricaFixaService funcionarioRubricaFixaService;

    @Test
    void criar_persisteRubricaFixaComValor() {
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("500.00"), "Ajuda de custo");

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobreposta(
            eq(FUNCIONARIO_ID), eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), isNull()))
            .thenReturn(false);
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class))).thenAnswer(inv -> {
            FuncionarioRubricaFixa saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        FuncionarioRubricaFixaDTO result = funcionarioRubricaFixaService.criar(dto);

        assertEquals(100L, result.id());
        assertEquals(new BigDecimal("500.00"), result.valor());
        assertEquals("900", result.rubricaCodigo());

        ArgumentCaptor<FuncionarioRubricaFixa> captor = ArgumentCaptor.forClass(FuncionarioRubricaFixa.class);
        verify(funcionarioRubricaFixaRepository).save(captor.capture());
        assertEquals(new BigDecimal("500.00"), captor.getValue().getValor());
    }

    @Test
    void criar_global_semFuncionario_persisteFuncionarioNull() {
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(null, new BigDecimal("500.00"), "Global RH");

        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobrepostaGlobal(
            eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), isNull()))
            .thenReturn(false);
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class))).thenAnswer(inv -> {
            FuncionarioRubricaFixa saved = inv.getArgument(0);
            saved.setId(200L);
            return saved;
        });

        FuncionarioRubricaFixaDTO result = funcionarioRubricaFixaService.criar(dto);

        assertEquals(200L, result.id());
        assertNull(result.funcionarioId());
        assertNull(result.funcionarioNome());

        ArgumentCaptor<FuncionarioRubricaFixa> captor = ArgumentCaptor.forClass(FuncionarioRubricaFixa.class);
        verify(funcionarioRubricaFixaRepository).save(captor.capture());
        assertNull(captor.getValue().getFuncionario());
        verify(funcionarioConsultaPort, never()).findByIdAndAtivoTrue(any());
    }

    @Test
    void criar_global_vigenciaSobreposta_lanca409Global() {
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(null, new BigDecimal("500.00"), null);

        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobrepostaGlobal(
            eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), isNull()))
            .thenReturn(true);

        FuncionarioRubricaFixaVigenciaConflictException ex = assertThrows(
            FuncionarioRubricaFixaVigenciaConflictException.class,
            () -> funcionarioRubricaFixaService.criar(dto));
        assertEquals(
            "Já existe rubrica fixa global ativa com vigência sobreposta para esta rubrica",
            ex.getMessage());
    }

    @Test
    void criar_individual_comportamentoInalterado() {
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("688.00"), null);

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobreposta(
            eq(FUNCIONARIO_ID), eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), isNull()))
            .thenReturn(false);
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class))).thenAnswer(inv -> {
            FuncionarioRubricaFixa saved = inv.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        FuncionarioRubricaFixaDTO result = funcionarioRubricaFixaService.criar(dto);

        assertEquals(FUNCIONARIO_ID, result.funcionarioId());
        assertEquals("Funcionário Teste", result.funcionarioNome());
        assertEquals(new BigDecimal("688.00"), result.valor());

        ArgumentCaptor<FuncionarioRubricaFixa> captor = ArgumentCaptor.forClass(FuncionarioRubricaFixa.class);
        verify(funcionarioRubricaFixaRepository).save(captor.capture());
        assertEquals(FUNCIONARIO_ID, captor.getValue().getFuncionario().getId());
        verify(funcionarioRubricaFixaRepository, never()).existsVigenciaSobrepostaGlobal(
            any(), any(), any(), any());
    }

    @Test
    void criar_global_semValor_rubricaNaoCalculada_lanca400() {
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(null, null, null);

        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));

        assertThrows(IllegalArgumentException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void toDTO_incluiPorcentagemDaRubrica() {
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        rubrica.setPorcentagem(138.63);
        FuncionarioRubricaFixa entity = new FuncionarioRubricaFixa();
        entity.setId(50L);
        entity.setFuncionario(funcionario);
        entity.setRubrica(rubrica);
        entity.setValor(new BigDecimal("500.00"));
        entity.setVigenciaInicio(VIGENCIA_INICIO);
        entity.setAtivo(true);

        when(funcionarioRubricaFixaRepository.findById(50L)).thenReturn(Optional.of(entity));

        FuncionarioRubricaFixaDTO result = funcionarioRubricaFixaService.buscarPorId(50L);

        assertEquals(138.63, result.porcentagem());
    }

    @Test
    void criar_semValorParaRubricaNaoCalculada_lancaIllegalArgument() {
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, null, null);

        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));

        assertThrows(IllegalArgumentException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void criar_vigenciaSobreposta_lanca409() {
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("500.00"), null);

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobreposta(
            eq(FUNCIONARIO_ID), eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), isNull()))
            .thenReturn(true);

        FuncionarioRubricaFixaVigenciaConflictException ex = assertThrows(
            FuncionarioRubricaFixaVigenciaConflictException.class,
            () -> funcionarioRubricaFixaService.criar(dto));
        assertEquals(
            "Já existe rubrica fixa ativa com vigência sobreposta para este funcionário e rubrica",
            ex.getMessage());
    }

    @Test
    void atualizar_global_semFuncionario_persisteFuncionarioNull() {
        Long id = 200L;
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(null, new BigDecimal("500.00"), "Global RH");
        FuncionarioRubricaFixa entity = entityAtiva(id, funcionario(), rubrica);

        when(funcionarioRubricaFixaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobrepostaGlobal(
            eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), eq(id)))
            .thenReturn(false);
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        FuncionarioRubricaFixaDTO result = funcionarioRubricaFixaService.atualizar(id, dto);

        assertEquals(id, result.id());
        assertNull(result.funcionarioId());
        assertNull(result.funcionarioNome());

        ArgumentCaptor<FuncionarioRubricaFixa> captor = ArgumentCaptor.forClass(FuncionarioRubricaFixa.class);
        verify(funcionarioRubricaFixaRepository).save(captor.capture());
        assertNull(captor.getValue().getFuncionario());
        verify(funcionarioConsultaPort, never()).findByIdAndAtivoTrue(any());
    }

    @Test
    void atualizar_global_vigenciaSobreposta_lanca409Global() {
        Long id = 200L;
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(null, new BigDecimal("500.00"), null);
        FuncionarioRubricaFixa entity = entityAtiva(id, null, rubrica);

        when(funcionarioRubricaFixaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobrepostaGlobal(
            eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), eq(id)))
            .thenReturn(true);

        FuncionarioRubricaFixaVigenciaConflictException ex = assertThrows(
            FuncionarioRubricaFixaVigenciaConflictException.class,
            () -> funcionarioRubricaFixaService.atualizar(id, dto));
        assertEquals(
            "Já existe rubrica fixa global ativa com vigência sobreposta para esta rubrica",
            ex.getMessage());
    }

    @Test
    void atualizar_individual_comportamentoInalterado() {
        Long id = 101L;
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("688.00"), null);
        FuncionarioRubricaFixa entity = entityAtiva(id, funcionario, rubrica);

        when(funcionarioRubricaFixaRepository.findById(id)).thenReturn(Optional.of(entity));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobreposta(
            eq(FUNCIONARIO_ID), eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), eq(id)))
            .thenReturn(false);
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        FuncionarioRubricaFixaDTO result = funcionarioRubricaFixaService.atualizar(id, dto);

        assertEquals(FUNCIONARIO_ID, result.funcionarioId());
        assertEquals("Funcionário Teste", result.funcionarioNome());
        assertEquals(new BigDecimal("688.00"), result.valor());

        ArgumentCaptor<FuncionarioRubricaFixa> captor = ArgumentCaptor.forClass(FuncionarioRubricaFixa.class);
        verify(funcionarioRubricaFixaRepository).save(captor.capture());
        assertEquals(FUNCIONARIO_ID, captor.getValue().getFuncionario().getId());
        verify(funcionarioRubricaFixaRepository, never()).existsVigenciaSobrepostaGlobal(
            any(), any(), any(), any());
    }

    @Test
    void buscarPorId_inexistente_lancaNotFound() {
        when(funcionarioRubricaFixaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FuncionarioRubricaFixaNotFoundException.class,
            () -> funcionarioRubricaFixaService.buscarPorId(99L));
    }

    @Test
    void criar_funcionarioInexistente_lancaNotFound() {
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("500.00"), null);

        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica("900", false)));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        assertThrows(FuncionarioNotFoundException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void criar_rubricaInexistente_lancaNotFound() {
        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("500.00"), null);

        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.empty());

        assertThrows(RubricaNotFoundException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void criar_rubricaCalculada_valorNullPermitido() {
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica("5000", true)));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario()));
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class))).thenAnswer(inv -> {
            FuncionarioRubricaFixa e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, null, null);
        assertEquals(null, funcionarioRubricaFixaService.criar(dto).valor());
    }

    @Test
    void criar_vigenciaFimAntesInicio_lancaExcecao() {
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica("900", false)));

        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, new BigDecimal("100.00"),
            VIGENCIA_INICIO, VIGENCIA_INICIO.minusDays(1), null, true, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void isRubricaCalculada_codigoNull_retornaFalse() {
        Rubrica rubrica = rubrica("900", false);
        rubrica.setCodigo(null);
        assertFalse(funcionarioRubricaFixaService.isRubricaCalculada(rubrica));
    }

    @Test
    void buscarPorId_inativo_lancaNotFound() {
        FuncionarioRubricaFixa inativa = entityAtiva(1L, funcionario(), rubrica("900", false));
        inativa.setAtivo(false);
        when(funcionarioRubricaFixaRepository.findById(1L)).thenReturn(Optional.of(inativa));

        assertThrows(FuncionarioRubricaFixaNotFoundException.class, () ->
            funcionarioRubricaFixaService.buscarPorId(1L));
    }

    @Test
    void remover_desativaRegistro() {
        FuncionarioRubricaFixa entity = entityAtiva(1L, funcionario(), rubrica("900", false));
        when(funcionarioRubricaFixaRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(funcionarioRubricaFixaRepository.save(entity)).thenReturn(entity);

        funcionarioRubricaFixaService.remover(1L);

        assertFalse(entity.getAtivo());
    }

    @Test
    void criar_vigenciaFimNull_permitido() {
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica("900", false)));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario()));
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class))).thenAnswer(inv -> {
            FuncionarioRubricaFixa e = inv.getArgument(0);
            e.setId(50L);
            return e;
        });

        FuncionarioRubricaFixaDTO dto = dto(FUNCIONARIO_ID, new BigDecimal("100.00"), null);
        assertEquals(null, funcionarioRubricaFixaService.criar(dto).vigenciaFim());
    }

    @Test
    void criar_vigenciaFimValida_permitido() {
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica("900", false)));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario()));
        when(funcionarioRubricaFixaRepository.save(any(FuncionarioRubricaFixa.class))).thenAnswer(inv -> {
            FuncionarioRubricaFixa e = inv.getArgument(0);
            e.setId(51L);
            return e;
        });

        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, new BigDecimal("100.00"),
            VIGENCIA_INICIO, VIGENCIA_INICIO.plusMonths(1), null, true, null, null, null, null);

        assertEquals(VIGENCIA_INICIO.plusMonths(1), funcionarioRubricaFixaService.criar(dto).vigenciaFim());
    }

    private FuncionarioRubricaFixaDTO dto(Long funcionarioId, BigDecimal valor, String comentario) {
        return new FuncionarioRubricaFixaDTO(
            null, funcionarioId, RUBRICA_ID, valor,
            VIGENCIA_INICIO, null, comentario, true, null, null, null, null);
    }

    private Funcionario funcionario() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(FUNCIONARIO_ID);
        funcionario.setNome("Funcionário Teste");
        return funcionario;
    }

    private Rubrica rubrica(String codigo, boolean calculada) {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("PROVENTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setId(RUBRICA_ID);
        rubrica.setCodigo(calculada ? "5000" : codigo);
        rubrica.setDescricao("Rubrica " + codigo);
        rubrica.setTipoRubrica(tipo);
        rubrica.setAtivo(true);
        return rubrica;
    }

    private FuncionarioRubricaFixa entityAtiva(Long id, Funcionario funcionario, Rubrica rubrica) {
        FuncionarioRubricaFixa entity = new FuncionarioRubricaFixa();
        entity.setId(id);
        entity.setFuncionario(funcionario);
        entity.setRubrica(rubrica);
        entity.setValor(new BigDecimal("500.00"));
        entity.setVigenciaInicio(VIGENCIA_INICIO);
        entity.setAtivo(true);
        return entity;
    }
}
