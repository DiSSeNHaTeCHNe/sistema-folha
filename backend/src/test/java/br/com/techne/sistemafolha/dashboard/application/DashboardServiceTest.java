package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;
    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private FolhaTotalizacaoPort folhaTotalizacaoPort;

    @Mock
    private CadastrosImportLookupPort cadastrosImportLookupPort;

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @Mock
    private DashboardAccessGuard dashboardAccessGuard;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getStats_restritoComCentrosVazios_retornaEmptyENaoChamaPortsDeDados() {
        stubAccessDenied();

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEmptyStats(stats);
        verify(folhaConsultaPort, never()).findResumoMaisRecente();
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), anyBoolean(), any());
        verify(cadastrosImportLookupPort, never()).countFuncionariosAtivos();
        verify(beneficioConsultaPort, never()).contarLancamentosAtivosNaCompetencia(any(), any());
    }

    @Test
    void getStats_semFuncionario_retornaEmptyENaoChamaPortsDeDados() {
        stubAccessDenied();

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEmptyStats(stats);
        verify(folhaConsultaPort, never()).findResumoMaisRecente();
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), anyBoolean(), any());
        verify(cadastrosImportLookupPort, never()).countFuncionariosAtivos();
    }

    @Test
    void getStats_acessoTotal_preservaAgregacaoGlobalComResumo() {
        stubAccessTotal();

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("50000.00"), 1, false,
            new BigDecimal("1000.00"));
        FolhaLinhaSnapshot linha = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));
        FolhaEvolucaoSnapshot evolucao = new FolhaEvolucaoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("50000.00"), 1, false);

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), isNull()))
            .thenReturn(List.of(linha));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            eq(List.of(linha)), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), any()))
            .thenReturn(new BigDecimal("5500.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(5L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(evolucao));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("5500.00"), stats.custoMensalFolha());
        assertEquals(5L, stats.totalBeneficiosAtivos());
        assertEquals(1, stats.evolucaoMensal().size());
        assertEquals(new BigDecimal("50000.00"), stats.evolucaoMensal().get(0).valorTotal());
        assertEquals(1, stats.evolucaoMensal().get(0).quantidadeFuncionarios());
        verify(beneficioConsultaPort).contarLancamentosAtivosNaCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM));
        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), isNull());
        verify(folhaConsultaPort).findEvolucaoUltimos12Meses(any());
    }

    @Test
    void getStats_restritoComCentros_calculaEvolucaoScopedNaoGlobal() {
        Set<Long> centros = Set.of(10L);
        stubAccessRestrito(centros);

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("99999.00"), 10, false,
            new BigDecimal("5000.00"));
        FolhaLinhaSnapshot linhaProvento = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));
        FolhaLinhaSnapshot linhaDesconto = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            2L, "002", "INSS", "DESCONTO", new BigDecimal("500.00"));
        FolhaEvolucaoSnapshot evolucaoGlobal = new FolhaEvolucaoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("99999.00"), 10, false);

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(centros)))
            .thenReturn(List.of(linhaProvento, linhaDesconto));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            any(), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), any()))
            .thenReturn(new BigDecimal("5200.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
            .thenReturn(2L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(evolucaoGlobal));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("5200.00"), stats.custoMensalFolha());
        assertEquals(2L, stats.totalBeneficiosAtivos());
        assertEquals(1, stats.evolucaoMensal().size());
        assertEquals(new BigDecimal("5200.00"), stats.evolucaoMensal().get(0).valorTotal());
        assertEquals(1, stats.evolucaoMensal().get(0).quantidadeFuncionarios());
        assertTrue(stats.evolucaoMensal().get(0).valorTotal().compareTo(evolucaoGlobal.totalLiquido()) != 0);
        assertTrue(!stats.evolucaoMensal().get(0).quantidadeFuncionarios()
            .equals(evolucaoGlobal.totalEmpregados()));
        verify(folhaConsultaPort, atLeastOnce()).findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(centros));
        verify(folhaConsultaPort).findEvolucaoUltimos12Meses(any());
        verify(beneficioConsultaPort, never()).contarLancamentosAtivosNaCompetencia(any(), any());
    }

    @Test
    void getStats_restritoComCentros_competenciaSemLinhas_pontoZeroNaEvolucao() {
        Set<Long> centros = Set.of(10L);
        stubAccessRestrito(centros);

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("99999.00"), 10, false,
            BigDecimal.ZERO);
        FolhaEvolucaoSnapshot evolucaoSemLinhas = new FolhaEvolucaoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("99999.00"), 10, false);

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(centros)))
            .thenReturn(List.of());
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
            .thenReturn(0L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(evolucaoSemLinhas));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            any(), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), any()))
            .thenReturn(BigDecimal.ZERO);

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1, stats.evolucaoMensal().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.evolucaoMensal().get(0).valorTotal()));
        assertEquals(0, stats.evolucaoMensal().get(0).quantidadeFuncionarios());
    }

    @Test
    void getStats_loginAusente_retornaEmpty() {
        when(dashboardAccessGuard.resolve(null))
            .thenReturn(DashboardAccessGuard.ResolvedDashboardAccess.accessDenied());
        DashboardStatsDTO stats = dashboardService.getStats(null);

        assertEmptyStats(stats);
        verify(dashboardAccessGuard).resolve(null);
        verify(folhaConsultaPort, never()).findResumoMaisRecente();
    }

    @Test
    void getStats_acessoTotalSemResumo_usaCountCadastros() {
        stubAccessTotal();
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.empty());
        when(cadastrosImportLookupPort.countFuncionariosAtivos()).thenReturn(10L);
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(any(), any()))
            .thenReturn(7L);

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(10L, stats.totalFuncionarios());
        assertEquals(7L, stats.totalBeneficiosAtivos());
        assertEquals(BigDecimal.ZERO, stats.custoMensalFolha());
        verify(beneficioConsultaPort).contarLancamentosAtivosNaCompetencia(any(), any());
    }

    @Test
    void getStats_acessoTotal_evolucaoExcluiDecimoTerceiro_quandoPortRetornaApenasRegulares() {
        stubAccessTotal();

        LocalDate dezInicio = LocalDate.of(2024, 12, 1);
        LocalDate dezFim = LocalDate.of(2024, 12, 31);
        BigDecimal totalRegularDez = new BigDecimal("50000.00");
        BigDecimal totalDecimoTerceiroDez = new BigDecimal("80000.00");

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            dezInicio, dezFim, totalRegularDez, 100, false, new BigDecimal("2000.00"));
        FolhaLinhaSnapshot linha = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));
        FolhaEvolucaoSnapshot evolucaoRegularDez = new FolhaEvolucaoSnapshot(
            dezInicio, dezFim, totalRegularDez, 100, false);

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(dezInicio), eq(dezFim), eq(false), isNull()))
            .thenReturn(List.of(linha));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            eq(List.of(linha)), eq(dezInicio), eq(dezFim), any()))
            .thenReturn(new BigDecimal("5300.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(dezInicio, dezFim))
            .thenReturn(5L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any()))
            .thenReturn(List.of(evolucaoRegularDez));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1, stats.evolucaoMensal().size());
        assertEquals(totalRegularDez, stats.evolucaoMensal().get(0).valorTotal());
        assertEquals(100, stats.evolucaoMensal().get(0).quantidadeFuncionarios());
        assertTrue(stats.evolucaoMensal().stream()
            .noneMatch(e -> e.valorTotal().compareTo(totalDecimoTerceiroDez) == 0));
        verify(folhaConsultaPort).findEvolucaoUltimos12Meses(any());
    }

    @Test
    void getStats_restritoComCentros_evolucaoExcluiDecimoTerceiro_quandoPortRetornaApenasRegulares() {
        Set<Long> centros = Set.of(10L);
        stubAccessRestrito(centros);

        LocalDate dezInicio = LocalDate.of(2024, 12, 1);
        LocalDate dezFim = LocalDate.of(2024, 12, 31);
        BigDecimal totalRegularDez = new BigDecimal("50000.00");
        BigDecimal totalDecimoTerceiroDez = new BigDecimal("80000.00");

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            dezInicio, dezFim, new BigDecimal("99999.00"), 10, false, BigDecimal.ZERO);
        FolhaLinhaSnapshot linhaProvento = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));
        FolhaLinhaSnapshot linhaDesconto = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            2L, "002", "INSS", "DESCONTO", new BigDecimal("500.00"));
        FolhaEvolucaoSnapshot evolucaoRegularDez = new FolhaEvolucaoSnapshot(
            dezInicio, dezFim, totalRegularDez, 100, false);

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(dezInicio), eq(dezFim), eq(false), eq(centros)))
            .thenReturn(List.of(linhaProvento, linhaDesconto));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            any(), eq(dezInicio), eq(dezFim), any()))
            .thenReturn(new BigDecimal("5200.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            dezInicio, dezFim, centros))
            .thenReturn(2L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any()))
            .thenReturn(List.of(evolucaoRegularDez));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1, stats.evolucaoMensal().size());
        assertEquals(new BigDecimal("5200.00"), stats.evolucaoMensal().get(0).valorTotal());
        assertEquals(1, stats.evolucaoMensal().get(0).quantidadeFuncionarios());
        assertTrue(stats.evolucaoMensal().stream()
            .noneMatch(e -> e.valorTotal().compareTo(totalDecimoTerceiroDez) == 0));
        verify(folhaConsultaPort).findEvolucaoUltimos12Meses(any());
        verify(folhaConsultaPort, atLeastOnce()).findLinhasAtivasPorCompetencia(
            eq(dezInicio), eq(dezFim), eq(false), eq(centros));
    }

    @Test
    void getStats_acessoTotal_semResumo_incluiBeneficios() {
        stubAccessTotal();
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.empty());
        when(cadastrosImportLookupPort.countFuncionariosAtivos()).thenReturn(3L);
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(any(), any()))
            .thenReturn(12L);

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(3L, stats.totalFuncionarios());
        assertEquals(12L, stats.totalBeneficiosAtivos());
        verify(beneficioConsultaPort, never()).contarLancamentosAtivosNaCompetenciaPorCentros(
            any(), any(), any());
    }

    @Test
    void getStats_acessoTotal_comResumo_agregaPorLinhaCentroCargoETops() {
        stubAccessTotal();

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("50000.00"), 2, false,
            new BigDecimal("1000.00"));
        FolhaLinhaSnapshot linha1 = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));
        FolhaLinhaSnapshot linha2 = linha(2L, 10L, "Centro A", 1L, "Linha A", 101L, "Dev",
            2L, "002", "INSS", "DESCONTO", new BigDecimal("500.00"));
        FolhaLinhaSnapshot linha3 = linha(2L, 20L, "Centro B", 2L, "Linha B", 101L, "Dev",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("3000.00"));

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), isNull()))
            .thenReturn(List.of(linha1, linha2, linha3));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(any(), any(), any(), any()))
            .thenReturn(new BigDecimal("8000.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(4L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(2L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("8000.00"), stats.totalProventos());
        assertEquals(new BigDecimal("500.00"), stats.totalDescontos());
        assertEquals(2, stats.porLinhaNegocio().size());
        assertEquals(2, stats.porCentroCusto().size());
        assertEquals(2, stats.porCargo().size());
        assertFalse(stats.topProventos().isEmpty());
        assertFalse(stats.topDescontos().isEmpty());
    }

    @Test
    void getStats_usuarioInexistente_retornaEmpty() {
        stubAccessDenied("ghost");

        DashboardStatsDTO stats = dashboardService.getStats("ghost");

        assertEmptyStats(stats);
    }

    @Test
    void getStats_loginBlank_retornaEmpty() {
        stubAccessDenied("   ");
        assertEmptyStats(dashboardService.getStats("   "));
    }

    @Test
    void getStats_motivoNegacaoExplicito_retornaEmpty() {
        stubAccessDenied();

        assertEmptyStats(dashboardService.getStats(LOGIN));
    }

    @Test
    void getStats_scopedCentrosVazios_retornaEmpty() {
        stubAccessDenied();

        assertEmptyStats(dashboardService.getStats(LOGIN));
    }

    @Test
    void getStats_semOrganograma_retornaEmpty() {
        stubAccessDenied();

        assertEmptyStats(dashboardService.getStats(LOGIN));
    }

    @Test
    void getStats_restritoSemResumo_contaFuncionariosPorCentros() {
        stubAccessRestrito(Set.of(10L));
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.empty());
        when(cadastrosImportLookupPort.countFuncionariosAtivosPorCentros(Set.of(10L))).thenReturn(3L);
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            any(), any(), eq(Set.of(10L)))).thenReturn(2L);

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(3L, stats.totalFuncionarios());
        assertEquals(2L, stats.totalBeneficiosAtivos());
    }

    @Test
    void getStats_comResumo_filtraLinhasSemIdsOpcionais() {
        stubAccessTotal();
        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 31),
            BigDecimal.TEN, 1, false, BigDecimal.ONE);
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        FolhaLinhaSnapshot linhaSemIds = linha(
            null, null, null, null, null, null, null,
            null, null, null, "PROVENTO", null);
        FolhaLinhaSnapshot linhaCompleta = linha(
            1L, 10L, "CC", 20L, "LN", 30L, "Cargo",
            40L, "R001", "Salário", "PROVENTO", BigDecimal.TEN);
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            resumo.competenciaInicio(), resumo.competenciaFim(), false, null))
            .thenReturn(List.of(linhaSemIds, linhaCompleta));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(any(), any(), any(), any()))
            .thenReturn(BigDecimal.TEN);
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(any(), any())).thenReturn(0L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1L, stats.totalFuncionarios());
        assertFalse(stats.porLinhaNegocio().isEmpty());
        assertFalse(stats.topProventos().isEmpty());
    }

    @Test
    void getStats_centrosNullNoContexto_retornaEmpty() {
        stubAccessDenied();

        assertEmptyStats(dashboardService.getStats(LOGIN));
    }

    @Test
    void getStats_comFuncionarioSemNoOrganograma_retornaEmpty() {
        stubAccessDenied();

        assertEmptyStats(dashboardService.getStats(LOGIN));
    }

    @Test
    void getStats_topRubricasOrdenaELimitaCinco() {
        stubAccessTotal();

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, BigDecimal.TEN, 1, false, BigDecimal.ONE);
        List<FolhaLinhaSnapshot> linhas = new java.util.ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            linhas.add(linha(1L, 10L, "CC", 1L, "LN", 100L, "Cargo",
                (long) i, "00" + i, "Rub " + i, "PROVENTO", BigDecimal.valueOf(i * 100)));
            linhas.add(linha(1L, 10L, "CC", 1L, "LN", 100L, "Cargo",
                (long) (10 + i), "D0" + i, "Desc " + i, "DESCONTO", BigDecimal.valueOf(i * 10)));
        }
        linhas.add(linha(1L, 10L, "CC", 1L, "LN", 100L, "Cargo",
            null, null, "Sem rubrica", "DESCONTO", BigDecimal.ONE));

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(any(), any(), eq(false), isNull()))
            .thenReturn(linhas);
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(any(), any(), any(), any()))
            .thenReturn(BigDecimal.TEN);
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(any(), any())).thenReturn(0L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of());

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(5, stats.topProventos().size());
        assertEquals(5, stats.topDescontos().size());
        assertEquals(6L, stats.topProventos().get(0).id());
        assertEquals(16L, stats.topDescontos().get(0).id());
    }

    private void stubAccessDenied() {
        when(dashboardAccessGuard.resolve(LOGIN))
            .thenReturn(DashboardAccessGuard.ResolvedDashboardAccess.accessDenied());
    }

    private void stubAccessDenied(String login) {
        when(dashboardAccessGuard.resolve(login))
            .thenReturn(DashboardAccessGuard.ResolvedDashboardAccess.accessDenied());
    }

    private void stubAccessTotal() {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(false, USUARIO_ID, contextoAcessoTotal(), null));
    }

    private void stubAccessRestrito(Set<Long> centros) {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(false, USUARIO_ID, contextoRestrito(centros), centros));
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private void assertEmptyStats(DashboardStatsDTO stats) {
        assertEquals(0L, stats.totalFuncionarios());
        assertEquals(BigDecimal.ZERO, stats.custoMensalFolha());
        assertEquals(0L, stats.totalBeneficiosAtivos());
        assertTrue(stats.porLinhaNegocio().isEmpty());
        assertTrue(stats.porCentroCusto().isEmpty());
        assertTrue(stats.porCargo().isEmpty());
        assertTrue(stats.topProventos().isEmpty());
        assertTrue(stats.topDescontos().isEmpty());
        assertTrue(stats.evolucaoMensal().isEmpty());
    }

    private FolhaLinhaSnapshot linha(
            Long funcionarioId, Long centroId, String centroDesc,
            Long linhaId, String linhaDesc, Long cargoId, String cargoDesc,
            Long rubricaId, String rubricaCodigo, String rubricaDesc,
            String tipo, BigDecimal valor) {
        short ob = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        short ol = "DESCONTO".equals(tipo) ? (short) -1 : ("PROVENTO".equals(tipo) ? (short) 1 : (short) 0);
        short oc = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        return new FolhaLinhaSnapshot(
            funcionarioId, "Func " + funcionarioId, centroId, centroDesc, linhaId, linhaDesc,
            cargoId, cargoDesc, rubricaId, rubricaCodigo, rubricaDesc, tipo, valor,
            ob, ol, oc, OrigemLinha.FOLHA_ADP, null);
    }
}
