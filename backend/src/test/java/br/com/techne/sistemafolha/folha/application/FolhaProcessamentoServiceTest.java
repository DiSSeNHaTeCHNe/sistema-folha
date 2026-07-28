package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRubricaFixaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.folha.api.ProcessamentoOpcoes;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaProcessamentoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private FichaMensalRepository fichaMensalRepository;

    @Mock
    private FichaLinhaRepository fichaLinhaRepository;

    @Mock
    private FuncionarioRubricaFixaRepository funcionarioRubricaFixaRepository;

    @Mock
    private RubricaRepository rubricaRepository;

    @InjectMocks
    private FolhaProcessamentoService folhaProcessamentoService;

    @Test
    void processar_copiaAdpParaFichaComOperadoresSnapshot() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica desconto = rubricaDesconto(2L);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FolhaPagamento linhaInss = linhaAdp(funcionario, desconto, new BigDecimal("800.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario, linhaInss));
        when(funcionarioRubricaFixaRepository.findVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(1, resultado.totalFichas());
        assertEquals(2, resultado.totalLinhas());
        assertEquals(1, resultado.totalFuncionarios());

        verify(fichaMensalRepository).deleteByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(2)).save(linhaCaptor.capture());
        FichaLinha linhaDesconto = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOperadorLiquido() == -1)
            .findFirst()
            .orElseThrow();
        assertEquals(OrigemLinha.FOLHA_ADP, linhaDesconto.getOrigemLinha());
        assertEquals((short) 0, linhaDesconto.getOperadorBruto());
        assertEquals((short) -1, linhaDesconto.getOperadorLiquido());

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10000.00"), fichaFinal.getBruto());
        assertEquals(new BigDecimal("9200.00"), fichaFinal.getLiquido());
        assertEquals(new BigDecimal("10000.00"), fichaFinal.getCustoFolha());
    }

    @Test
    void processar_injetarCustoFixoVigente() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica ajuda = rubricaProvento(3L);
        ajuda.setCodigo("900");

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FuncionarioRubricaFixa fixo = rubricaFixa(funcionario, ajuda, new BigDecimal("500.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(funcionarioRubricaFixaRepository.findVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(fixo));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(2, resultado.totalLinhas());

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(2)).save(linhaCaptor.capture());
        FichaLinha linhaFixa = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CUSTO_FIXO)
            .findFirst()
            .orElseThrow();
        assertEquals(new BigDecimal("500.00"), linhaFixa.getValor());

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10500.00"), fichaFinal.getBruto());
        assertEquals(new BigDecimal("10500.00"), fichaFinal.getCustoFolha());
    }

    @Test
    void processar_custoFixoDuplicataAdp_prefereAdp() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FuncionarioRubricaFixa fixo = rubricaFixa(funcionario, provento, new BigDecimal("999.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(funcionarioRubricaFixaRepository.findVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(fixo));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(1, resultado.totalLinhas());
        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(1)).save(linhaCaptor.capture());
        assertEquals(OrigemLinha.FOLHA_ADP, linhaCaptor.getValue().getOrigemLinha());
        assertEquals(new BigDecimal("10000.00"), linhaCaptor.getValue().getValor());
    }

    @Test
    void processar_recalcularFerias_injetarLinhaCalculada() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica ferias = rubricaFerias();

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("12000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(funcionarioRubricaFixaRepository.findVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(rubricaRepository.findByCodigo("5000")).thenReturn(Optional.of(ferias));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(true));

        assertEquals(2, resultado.totalLinhas());

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(2)).save(linhaCaptor.capture());
        FichaLinha linhaFerias = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CALCULADO)
            .findFirst()
            .orElseThrow();
        assertEquals(new BigDecimal("2500.00"), linhaFerias.getValor());

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("14500.00"), fichaFinal.getBruto());
        assertEquals(new BigDecimal("14500.00"), fichaFinal.getCustoFolha());
    }

    private Rubrica rubricaFerias() {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("PROVENTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setId(50L);
        rubrica.setCodigo("5000");
        rubrica.setDescricao("Férias proporcionais");
        rubrica.setTipoRubrica(tipo);
        rubrica.setOperadorBruto((short) 1);
        rubrica.setOperadorLiquido((short) 1);
        rubrica.setOperadorCusto((short) 1);
        rubrica.setAtivo(true);
        return rubrica;
    }

    private FuncionarioRubricaFixa rubricaFixa(Funcionario funcionario, Rubrica rubrica, BigDecimal valor) {
        FuncionarioRubricaFixa fixo = new FuncionarioRubricaFixa();
        fixo.setFuncionario(funcionario);
        fixo.setRubrica(rubrica);
        fixo.setValor(valor);
        fixo.setVigenciaInicio(COMPETENCIA_INICIO);
        fixo.setAtivo(true);
        return fixo;
    }

    private Funcionario funcionario(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("Funcionário " + id);
        return funcionario;
    }

    private Rubrica rubricaProvento(Long id) {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("PROVENTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setId(id);
        rubrica.setTipoRubrica(tipo);
        rubrica.setOperadorBruto((short) 1);
        rubrica.setOperadorLiquido((short) 1);
        rubrica.setOperadorCusto((short) 1);
        return rubrica;
    }

    private Rubrica rubricaDesconto(Long id) {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("DESCONTO");
        Rubrica rubrica = new Rubrica();
        rubrica.setId(id);
        rubrica.setTipoRubrica(tipo);
        rubrica.setOperadorBruto((short) 0);
        rubrica.setOperadorLiquido((short) -1);
        rubrica.setOperadorCusto((short) 0);
        return rubrica;
    }

    private FolhaPagamento linhaAdp(Funcionario funcionario, Rubrica rubrica, BigDecimal valor) {
        FolhaPagamento linha = new FolhaPagamento();
        linha.setFuncionario(funcionario);
        linha.setRubrica(rubrica);
        linha.setValor(valor);
        linha.setDataInicio(COMPETENCIA_INICIO);
        linha.setDataFim(COMPETENCIA_FIM);
        linha.setAtivo(true);
        linha.setDecimoTerceiro(false);
        return linha;
    }
}
