package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import br.com.techne.sistemafolha.cadastros.domain.RegimeTrabalho;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private CadastrosLookupPort cadastrosLookupPort;

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
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
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
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
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
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
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
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(cadastrosLookupPort.findRubricaAtivaByCodigo("5000")).thenReturn(Optional.of(ferias));
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

    @Test
    void processar_alteracaoCadastroFixo_naoAtualizaFichaAteReprocessar() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica ajuda = rubricaProvento(3L);
        ajuda.setCodigo("900");

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FuncionarioRubricaFixa fixo500 = rubricaFixa(funcionario, ajuda, new BigDecimal("500.00"));
        FuncionarioRubricaFixa fixo800 = rubricaFixa(funcionario, ajuda, new BigDecimal("800.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(fixo500));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaAposPrimeiroProcessamento =
            fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10500.00"), fichaAposPrimeiroProcessamento.getBruto());
        assertEquals(new BigDecimal("10500.00"), fichaAposPrimeiroProcessamento.getCustoFolha());

        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(fixo800));

        assertEquals(new BigDecimal("10500.00"), fichaAposPrimeiroProcessamento.getBruto());
        assertEquals(new BigDecimal("10500.00"), fichaAposPrimeiroProcessamento.getCustoFolha());

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(4)).save(fichaCaptor.capture());
        FichaMensal fichaAposReprocessamento =
            fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10800.00"), fichaAposReprocessamento.getBruto());
        assertEquals(new BigDecimal("10800.00"), fichaAposReprocessamento.getCustoFolha());
    }

    @Test
    void processar_copiaPorcentagemSnapshotAdpCustoFixoECalculado() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        provento.setPorcentagem(138.63);
        Rubrica ajuda = rubricaProvento(3L);
        ajuda.setCodigo("900");
        ajuda.setPorcentagem(100.0);
        Rubrica ferias = rubricaFerias();
        ferias.setPorcentagem(50.0);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("7258.43"));
        FuncionarioRubricaFixa fixo = rubricaFixa(funcionario, ajuda, new BigDecimal("688.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(fixo));
        when(cadastrosLookupPort.findRubricaAtivaByCodigo("5000")).thenReturn(Optional.of(ferias));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(true));

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(3)).save(linhaCaptor.capture());

        FichaLinha linhaAdp = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.FOLHA_ADP)
            .findFirst().orElseThrow();
        assertEquals(new BigDecimal("138.63"), linhaAdp.getPorcentagem());

        FichaLinha linhaFixa = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CUSTO_FIXO)
            .findFirst().orElseThrow();
        assertEquals(new BigDecimal("100.0"), linhaFixa.getPorcentagem());

        FichaLinha linhaFerias = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CALCULADO)
            .findFirst().orElseThrow();
        assertEquals(new BigDecimal("50.0"), linhaFerias.getPorcentagem());
    }

    @Test
    void processar_custoFixo688Porcentagem100_custoFolhaInclui688BrutoUsaValorOriginal() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica fixaRh = rubricaProvento(3L);
        fixaRh.setCodigo("RH");
        fixaRh.setPorcentagem(100.0);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FuncionarioRubricaFixa fixo = rubricaFixa(funcionario, fixaRh, new BigDecimal("688.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(fixo));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10688.00"), fichaFinal.getBruto());
        assertEquals(new BigDecimal("10688.00"), fichaFinal.getCustoFolha());
    }

    @Test
    void processar_porcentagem13863_persisteCustoFolhaComFormula() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        provento.setPorcentagem(138.63);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("7258.43"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("7258.43"), fichaFinal.getBruto());
        assertEquals(new BigDecimal("10062.36"), fichaFinal.getCustoFolha());
    }

    @Test
    void processar_reprocessoAposAlterarPorcentagemCadastro_refleteNovaPorcentagem() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        provento.setPorcentagem(100.0);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("7258.43"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaPrimeiro = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("7258.43"), fichaPrimeiro.getCustoFolha());

        provento.setPorcentagem(138.63);
        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(4)).save(fichaCaptor.capture());
        FichaMensal fichaReprocesso = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10062.36"), fichaReprocesso.getCustoFolha());
        assertEquals(new BigDecimal("7258.43"), fichaReprocesso.getBruto());
    }

    @Test
    void processar_globalFixaVigente_aplicaEmDoisClt() {
        Funcionario func1 = funcionario(1L);
        Funcionario func2 = funcionario(2L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica ajuda = rubricaProvento(3L);
        ajuda.setCodigo("900");

        FolhaPagamento linha1 = linhaAdp(func1, provento, new BigDecimal("10000.00"));
        FolhaPagamento linha2 = linhaAdp(func2, provento, new BigDecimal("8000.00"));
        FuncionarioRubricaFixa global = rubricaFixaGlobal(ajuda, new BigDecimal("500.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha1, linha2));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(global));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(ficha.getFuncionario().getId() * 100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(2, resultado.totalFichas());
        assertEquals(4, resultado.totalLinhas());

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(4)).save(linhaCaptor.capture());
        long linhasCustoFixo = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CUSTO_FIXO)
            .count();
        assertEquals(2, linhasCustoFixo);
        linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CUSTO_FIXO)
            .forEach(l -> assertEquals(new BigDecimal("500.00"), l.getValor()));
    }

    @Test
    void processar_individualPrevaleceSobreGlobal_mesmaRubrica() {
        Funcionario func1 = funcionario(1L);
        Funcionario func2 = funcionario(2L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica rh = rubricaProvento(3L);
        rh.setCodigo("RH");

        FolhaPagamento linha1 = linhaAdp(func1, provento, new BigDecimal("10000.00"));
        FolhaPagamento linha2 = linhaAdp(func2, provento, new BigDecimal("8000.00"));
        FuncionarioRubricaFixa individual = rubricaFixa(func1, rh, new BigDecimal("688.00"));
        FuncionarioRubricaFixa global = rubricaFixaGlobal(rh, new BigDecimal("500.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha1, linha2));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(individual, global));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(ficha.getFuncionario().getId() * 100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaLinha> linhaCaptor = ArgumentCaptor.forClass(FichaLinha.class);
        verify(fichaLinhaRepository, times(4)).save(linhaCaptor.capture());

        FichaLinha fixaFunc1 = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CUSTO_FIXO
                && l.getFichaMensal().getFuncionario().getId().equals(1L))
            .findFirst().orElseThrow();
        assertEquals(new BigDecimal("688.00"), fixaFunc1.getValor());

        FichaLinha fixaFunc2 = linhaCaptor.getAllValues().stream()
            .filter(l -> l.getOrigemLinha() == OrigemLinha.CUSTO_FIXO
                && l.getFichaMensal().getFuncionario().getId().equals(2L))
            .findFirst().orElseThrow();
        assertEquals(new BigDecimal("500.00"), fixaFunc2.getValor());
    }

    @Test
    void processar_linhaAdpComCcDistintoDoFuncionario_persisteSnapshotCcDaLinha_fcc18() {
        CentroCusto ccLinha = centroCusto(100L);
        CentroCusto ccFuncionario = centroCusto(200L);

        Funcionario funcionario = funcionario(1L);
        funcionario.setCentroCusto(ccFuncionario);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        linhaSalario.setCentroCusto(ccLinha);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertNotNull(fichaFinal.getCentroCusto());
        assertEquals(100L, fichaFinal.getCentroCusto().getId());
    }

    @Test
    void processar_reprocessoAposAlterarCcLinhaAdp_atualizaSnapshotCc_fcc21() {
        CentroCusto ccAlpha = centroCusto(100L);
        CentroCusto ccBeta = centroCusto(200L);

        Funcionario funcionario = funcionario(1L);
        funcionario.setCentroCusto(ccBeta);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        linhaSalario.setCentroCusto(ccAlpha);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaPrimeiro = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(100L, fichaPrimeiro.getCentroCusto().getId());

        linhaSalario.setCentroCusto(ccBeta);
        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(4)).save(fichaCaptor.capture());
        FichaMensal fichaReprocesso = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(200L, fichaReprocesso.getCentroCusto().getId());
    }

    @Test
    void processar_semCcNaLinha_fallbackFuncionarioCentroCusto_fcc18() {
        CentroCusto ccFuncionario = centroCusto(200L);

        Funcionario funcionario = funcionario(1L);
        funcionario.setCentroCusto(ccFuncionario);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertNotNull(fichaFinal.getCentroCusto());
        assertEquals(200L, fichaFinal.getCentroCusto().getId());
    }

    @Test
    void processar_globalFixaDuplicataAdp_prefereAdp() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FuncionarioRubricaFixa global = rubricaFixaGlobal(provento, new BigDecimal("999.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(global));
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
    }

    @Test
    void processar_globalFixaAlteradaAposProcessamento_sóRefleteNoReprocesso() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        Rubrica ajuda = rubricaProvento(3L);
        ajuda.setCodigo("900");

        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        FuncionarioRubricaFixa global = rubricaFixaGlobal(ajuda, new BigDecimal("500.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(global));
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaPrimeiro = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10500.00"), fichaPrimeiro.getCustoFolha());

        global.setValor(new BigDecimal("700.00"));
        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(4)).save(fichaCaptor.capture());
        FichaMensal fichaReprocesso = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(new BigDecimal("10700.00"), fichaReprocesso.getCustoFolha());
    }

    @Test
    void processar_funcionarioNaoClt_ignoraGrupo() {
        Funcionario pj = funcionario(99L);
        RegimeTrabalho regime = new RegimeTrabalho();
        regime.setCodigo("PJ");
        regime.setAtivo(true);
        pj.setRegimeTrabalho(regime);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linha = linhaAdp(pj, provento, new BigDecimal("10000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(0, resultado.totalFichas());
        assertEquals(0, resultado.totalLinhas());
        verify(fichaLinhaRepository, times(0)).save(any(FichaLinha.class));
    }

    @Test
    void processar_regimeInativo_ignoraGrupo() {
        Funcionario funcionario = funcionario(1L);
        RegimeTrabalho regime = new RegimeTrabalho();
        regime.setCodigo("CLT");
        regime.setAtivo(false);
        funcionario.setRegimeTrabalho(regime);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linha = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null);

        assertEquals(0, resultado.totalFichas());
        verify(fichaLinhaRepository, times(0)).save(any(FichaLinha.class));
    }

    @Test
    void processar_recalcularFerias_rubricaNaoEncontrada_naoInjetaLinha() {
        Funcionario funcionario = funcionario(1L);
        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("12000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(cadastrosLookupPort.findRubricaAtivaByCodigo("5000")).thenReturn(Optional.empty());
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

        assertEquals(1, resultado.totalLinhas());
        verify(fichaLinhaRepository, times(1)).save(any(FichaLinha.class));
    }

    @Test
    void processar_semCentroCustoNaLinhaNemFuncionario_centroCustoNull() {
        Funcionario funcionario = funcionario(1L);
        funcionario.setCentroCusto(null);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(null, fichaFinal.getCentroCusto());
    }

    @Test
    void processar_centroCustoDaLinhaQuandoCoincideComEfetivo() {
        CentroCusto ccLinha = centroCusto(100L);
        Funcionario funcionario = funcionario(1L);
        funcionario.setCentroCusto(centroCusto(200L));

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linhaSalario = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        linhaSalario.setCentroCusto(ccLinha);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaSalario));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertEquals(100L, fichaFinal.getCentroCusto().getId());
        assertSame(ccLinha, fichaFinal.getCentroCusto());
    }

    @Test
    void processar_regimeCltCodigoErrado_ignoraGrupo() {
        Funcionario funcionario = funcionario(1L);
        RegimeTrabalho regime = new RegimeTrabalho();
        regime.setCodigo("ESTAGIO");
        regime.setAtivo(true);
        funcionario.setRegimeTrabalho(regime);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linha = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());

        ProcessamentoResultadoDTO resultado = folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        assertEquals(0, resultado.totalFichas());
    }

    @Test
    void processar_regimeNull_trataComoClt() {
        Funcionario funcionario = funcionario(1L);
        funcionario.setRegimeTrabalho(null);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linha = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
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
    }

    @Test
    void processar_centroCustoEfetivo_retornaCentroDoFuncionarioQuandoLinhaSemCc() {
        CentroCusto ccFuncionario = centroCusto(200L);
        Funcionario funcionario = funcionario(1L);
        funcionario.setCentroCusto(ccFuncionario);

        Rubrica provento = rubricaProvento(1L);
        FolhaPagamento linha = linhaAdp(funcionario, provento, new BigDecimal("10000.00"));
        linha.setCentroCusto(null);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha));
        when(cadastrosLookupPort.findRubricasFixasVigentesNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());
        when(fichaMensalRepository.save(any(FichaMensal.class))).thenAnswer(inv -> {
            FichaMensal ficha = inv.getArgument(0);
            if (ficha.getId() == null) {
                ficha.setId(100L);
            }
            return ficha;
        });
        when(fichaLinhaRepository.save(any(FichaLinha.class))).thenAnswer(inv -> inv.getArgument(0));

        folhaProcessamentoService.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));

        ArgumentCaptor<FichaMensal> fichaCaptor = ArgumentCaptor.forClass(FichaMensal.class);
        verify(fichaMensalRepository, org.mockito.Mockito.atLeast(2)).save(fichaCaptor.capture());
        FichaMensal fichaFinal = fichaCaptor.getAllValues().get(fichaCaptor.getAllValues().size() - 1);
        assertSame(ccFuncionario, fichaFinal.getCentroCusto());
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

    private FuncionarioRubricaFixa rubricaFixaGlobal(Rubrica rubrica, BigDecimal valor) {
        FuncionarioRubricaFixa fixo = new FuncionarioRubricaFixa();
        fixo.setFuncionario(null);
        fixo.setRubrica(rubrica);
        fixo.setValor(valor);
        fixo.setVigenciaInicio(COMPETENCIA_INICIO);
        fixo.setAtivo(true);
        return fixo;
    }

    private CentroCusto centroCusto(Long id) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(id);
        return centroCusto;
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
