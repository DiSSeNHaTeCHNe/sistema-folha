package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), any());
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
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), any());
        verify(cadastrosImportLookupPort, never()).countFuncionariosAtivos();
    }

    @Test
    void getStats_acessoTotal_preservaAgregacaoGlobalComResumo() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("50000.00"), 1, false);
        FolhaLinhaSnapshot linha = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));
        FolhaEvolucaoSnapshot evolucao = new FolhaEvolucaoSnapshot(
            COMPETENCIA_INICIO, new BigDecimal("50000.00"), 1);

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), isNull()))
            .thenReturn(List.of(linha));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(5L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(evolucao));

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("50000.00"), stats.custoMensalFolha());
        assertEquals(5L, stats.totalBeneficiosAtivos());
        assertEquals(1, stats.evolucaoMensal().size());
        verify(beneficioConsultaPort).contarLancamentosAtivosNaCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM));
        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), isNull());
    }

    @Test
    void getStats_restritoComCentros_filtraViaPortScopedEEvolucaoVazia() {
        stubUsuario();
        Set<Long> centros = Set.of(10L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoRestrito(centros));

        FolhaResumoSnapshot resumo = new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("99999.00"), 10, false);
        FolhaLinhaSnapshot linha = linha(1L, 10L, "Centro A", 1L, "Linha A", 100L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));

        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumo));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(centros)))
            .thenReturn(List.of(linha));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
            .thenReturn(2L);

        DashboardStatsDTO stats = dashboardService.getStats(LOGIN);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("5000.00"), stats.custoMensalFolha());
        assertEquals(2L, stats.totalBeneficiosAtivos());
        assertTrue(stats.evolucaoMensal().isEmpty());
        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(
            eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(centros));
        verify(folhaConsultaPort, never()).findEvolucaoUltimos12Meses(any());
        verify(beneficioConsultaPort, never()).contarLancamentosAtivosNaCompetencia(any(), any());
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
        return new FolhaLinhaSnapshot(
            funcionarioId, centroId, centroDesc, linhaId, linhaDesc,
            cargoId, cargoDesc, rubricaId, rubricaCodigo, rubricaDesc, tipo, valor);
    }
}
