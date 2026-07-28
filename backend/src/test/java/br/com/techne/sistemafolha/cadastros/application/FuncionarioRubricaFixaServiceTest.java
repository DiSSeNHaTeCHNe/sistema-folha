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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, new BigDecimal("500.00"),
            VIGENCIA_INICIO, null, "Ajuda de custo", true, null, null, null);

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
    void criar_semValorParaRubricaNaoCalculada_lancaIllegalArgument() {
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, null,
            VIGENCIA_INICIO, null, null, true, null, null, null);

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));

        assertThrows(IllegalArgumentException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void criar_vigenciaSobreposta_lanca409() {
        Funcionario funcionario = funcionario();
        Rubrica rubrica = rubrica("900", false);
        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, new BigDecimal("500.00"),
            VIGENCIA_INICIO, null, null, true, null, null, null);

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.of(rubrica));
        when(funcionarioRubricaFixaRepository.existsVigenciaSobreposta(
            eq(FUNCIONARIO_ID), eq(RUBRICA_ID), eq(VIGENCIA_INICIO), isNull(), isNull()))
            .thenReturn(true);

        assertThrows(FuncionarioRubricaFixaVigenciaConflictException.class,
            () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void buscarPorId_inexistente_lancaNotFound() {
        when(funcionarioRubricaFixaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FuncionarioRubricaFixaNotFoundException.class,
            () -> funcionarioRubricaFixaService.buscarPorId(99L));
    }

    @Test
    void criar_funcionarioInexistente_lancaNotFound() {
        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, new BigDecimal("500.00"),
            VIGENCIA_INICIO, null, null, true, null, null, null);

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        assertThrows(FuncionarioNotFoundException.class, () -> funcionarioRubricaFixaService.criar(dto));
    }

    @Test
    void criar_rubricaInexistente_lancaNotFound() {
        FuncionarioRubricaFixaDTO dto = new FuncionarioRubricaFixaDTO(
            null, FUNCIONARIO_ID, RUBRICA_ID, new BigDecimal("500.00"),
            VIGENCIA_INICIO, null, null, true, null, null, null);

        when(funcionarioConsultaPort.findByIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario()));
        when(rubricaRepository.findByIdAndAtivoTrue(RUBRICA_ID)).thenReturn(Optional.empty());

        assertThrows(RubricaNotFoundException.class, () -> funcionarioRubricaFixaService.criar(dto));
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
}
