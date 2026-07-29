package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private CadastrosImportLookupPort cadastrosImportLookupPort;

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getStats_restritoComCentrosVazios_retornaEmptyENaoChamaPortsDeDados() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of()));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEmptyStats(stats);
        verify(folhaConsultaPort, never()).findResumoMaisRecente();
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), anyBoolean(), any());
        verify(cadastrosImportLookupPort, never()).countFuncionariosAtivos();
        verify(beneficioConsultaPort, never()).contarLancamentosAtivosNaCompetencia(any(), any());
    }

    @Test
    void getStats_semFuncionario_retornaEmptyENaoChamaPortsDeDados() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(
                false, false, false, Set.of(), MotivoNegacaoAcesso.SEM_FUNCIONARIO, null, null, null));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEmptyStats(stats);
        verify(folhaConsultaPort, never()).findResumoMaisRecente();
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), anyBoolean(), any());
        verify(cadastrosImportLookupPort, never()).countFuncionariosAtivos();
    }

    @Test
    void getStats_acessoTotal_preservaAgregacaoGlobalComResumo() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

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
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
            eq(Set.of(1L)), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM)))
            .thenReturn(Map.of(1L, new BigDecimal("500.00")));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(5L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(evolucao));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("6500.00"), stats.custoMensalFolha());
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
        stubUsuario();
        Set<Long> centros = Set.of(10L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoRestrito(centros));

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
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
            eq(Set.of(1L)), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM)))
            .thenReturn(Map.of(1L, new BigDecimal("200.00")));
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
        stubUsuario();
        Set<Long> centros = Set.of(10L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoRestrito(centros));

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

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1, stats.evolucaoMensal().size());
        assertEquals(0, BigDecimal.ZERO.compareTo(stats.evolucaoMensal().get(0).valorTotal()));
        assertEquals(0, stats.evolucaoMensal().get(0).quantidadeFuncionarios());
    }

    @Test
    void getStats_loginAusente_retornaEmpty() {
        DashboardStatsDTO stats = dashboardService.getStats(null);

        assertEmptyStats(stats);
        verify(usuarioLookupPort, never()).findByLoginAndAtivoTrue(any());
        verify(folhaConsultaPort, never()).findResumoMaisRecente();
    }

    @Test
    void getStats_acessoTotalSemResumo_usaCountCadastros() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
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
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

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
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
            eq(Set.of(1L)), eq(dezInicio), eq(dezFim)))
            .thenReturn(Map.of(1L, new BigDecimal("300.00")));
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
        stubUsuario();
        Set<Long> centros = Set.of(10L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoRestrito(centros));

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
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
            eq(Set.of(1L)), eq(dezInicio), eq(dezFim)))
            .thenReturn(Map.of(1L, new BigDecimal("200.00")));
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
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
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

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
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
