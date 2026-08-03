package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.port.DashboardConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.relatorios.application.pdf.BeneficioCustoPdfRenderer;
import br.com.techne.sistemafolha.relatorios.application.pdf.FolhaExecutivoPdfRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioPdfServiceTest {

    private static final String LOGIN = "gestor@teste.com";
    private static final Long USUARIO_ID = 42L;
    private static final int MES = 6;
    private static final int ANO = 2024;

    @Mock
    private DashboardConsultaPort dashboardConsultaPort;
    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;
    @Mock
    private FolhaTotalizacaoPort folhaTotalizacaoPort;
    @Mock
    private FolhaConsultaPort folhaConsultaPort;
    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;
    @Mock
    private RelatorioBrandingService brandingService;
    @Mock
    private FolhaExecutivoPdfRenderer folhaExecutivoPdfRenderer;
    @Mock
    private BeneficioCustoPdfRenderer beneficioCustoPdfRenderer;

    @InjectMocks
    private RelatorioPdfService relatorioPdfService;

    private BrandingTheme theme;
    private LocalDate competenciaInicio;
    private LocalDate competenciaFim;

    @BeforeEach
    void setUp() {
        theme = new BrandingTheme("#7836FC", "#3661FC", "#273340", "#f8fafc", Optional.empty());
        when(brandingService.load()).thenReturn(theme);
        competenciaInicio = LocalDate.of(2024, 6, 1);
        competenciaFim = LocalDate.of(2024, 6, 30);
    }

    @Test
    void renderFolhaExecutivo_montaModelViaPortsEDelegaRenderer() {
        DashboardStatsDTO stats = statsComDados();
        List<EvolucaoMensalDTO> evolucao = List.of(
            new EvolucaoMensalDTO("Jun/2024", new BigDecimal("9000"), 150));
        when(dashboardConsultaPort.getStatsForCompetencia(
            eq(LOGIN), eq(competenciaInicio), eq(competenciaFim), eq(false)))
            .thenReturn(stats);
        when(dashboardConsultaPort.getEvolucaoMeses(
            eq(LOGIN), eq(competenciaFim), eq(6), eq(false)))
            .thenReturn(evolucao);
        when(folhaExecutivoPdfRenderer.render(any())).thenReturn("%PDF-folha".getBytes(StandardCharsets.UTF_8));

        byte[] pdf = relatorioPdfService.renderFolhaExecutivo(LOGIN, USUARIO_ID, MES, ANO);

        assertTrue(new String(pdf, StandardCharsets.UTF_8).startsWith("%PDF"));

        ArgumentCaptor<RelatorioFolhaModel> captor = ArgumentCaptor.forClass(RelatorioFolhaModel.class);
        verify(folhaExecutivoPdfRenderer).render(captor.capture());
        RelatorioFolhaModel model = captor.getValue();
        assertEquals("06/2024", model.competenciaLabel());
        assertEquals(LOGIN, model.geradoPor());
        assertEquals(stats, model.stats());
        assertEquals(evolucao, model.evolucao6Meses());
        assertFalse(model.semDados());
        verify(dashboardConsultaPort).getStatsForCompetencia(
            LOGIN, competenciaInicio, competenciaFim, false);
    }

    @Test
    void renderBeneficioCusto_calculaCustoConsolidadoViaPortsEDelegaRenderer() {
        AccessContextDTO contexto = new AccessContextDTO(
            true, true, false, Set.of(1L, 2L), null, 1L, "Diretoria", 1);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);

        List<FolhaLinhaSnapshot> linhas = List.of(
            new FolhaLinhaSnapshot(
                1L, "João", 1L, "CC1", 1L, "LN1", 1L, "Cargo",
                1L, "100", "Salário", "PROVENTO", new BigDecimal("5000"),
                (short) 1, (short) 1, (short) 1, OrigemLinha.FOLHA_ADP, null));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(competenciaInicio), eq(competenciaFim), eq(false), eq(Set.of(1L, 2L))))
            .thenReturn(linhas);
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            eq(linhas), eq(competenciaInicio), eq(competenciaFim), eq(contexto)))
            .thenReturn(new BigDecimal("8000.00"));

        when(beneficioConsultaPort.somarValorPorCompetenciaECentros(
            eq(competenciaInicio), eq(competenciaFim), eq(Set.of(1L, 2L))))
            .thenReturn(new BigDecimal("2000.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            eq(competenciaInicio), eq(competenciaFim), eq(Set.of(1L, 2L))))
            .thenReturn(5L);

        BeneficioTipoResumoSnapshot tipo = new BeneficioTipoResumoSnapshot(
            10L, "VT", "Vale Transporte", new BigDecimal("2000.00"), 5L);
        when(beneficioConsultaPort.resumoPorTipo(
            eq(competenciaInicio), eq(competenciaFim), eq(Set.of(1L, 2L))))
            .thenReturn(List.of(tipo));
        when(beneficioConsultaPort.topFuncionariosPorTipo(
            eq(10L), eq(competenciaInicio), eq(competenciaFim), eq(Set.of(1L, 2L)), eq(10)))
            .thenReturn(List.of());
        when(beneficioConsultaPort.matrizCentroCustoPorTipo(
            eq(competenciaInicio), eq(competenciaFim), eq(Set.of(1L, 2L)), eq(5), eq(5)))
            .thenReturn(List.of());

        when(beneficioCustoPdfRenderer.render(any())).thenReturn("%PDF-benef".getBytes(StandardCharsets.UTF_8));

        byte[] pdf = relatorioPdfService.renderBeneficioCusto(LOGIN, USUARIO_ID, MES, ANO);

        assertTrue(new String(pdf, StandardCharsets.UTF_8).startsWith("%PDF"));

        ArgumentCaptor<RelatorioBeneficioModel> captor = ArgumentCaptor.forClass(RelatorioBeneficioModel.class);
        verify(beneficioCustoPdfRenderer).render(captor.capture());
        RelatorioBeneficioModel model = captor.getValue();
        assertEquals(new BigDecimal("2000.00"), model.totalBeneficios());
        assertEquals(new BigDecimal("8000.00"), model.totalCustoFolha());
        assertEquals(new BigDecimal("10000.00"), model.custoConsolidado());
        assertFalse(model.semBeneficios());
        assertFalse(model.semFolha());
        verify(folhaTotalizacaoPort).calcularTotalCustoEmpresa(
            linhas, competenciaInicio, competenciaFim, contexto);
        verify(organogramaAcessoPort).obterContextoAcesso(USUARIO_ID);
    }

    @Test
    void renderBeneficioCusto_acessoTotal_somaBeneficiosGlobal() {
        AccessContextDTO contexto = new AccessContextDTO(
            true, true, true, null, null, null, null, null);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            eq(competenciaInicio), eq(competenciaFim), eq(false), eq(null)))
            .thenReturn(List.of());
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            anyList(), eq(competenciaInicio), eq(competenciaFim), eq(contexto)))
            .thenReturn(new BigDecimal("8000.00"));
        when(beneficioConsultaPort.somarValorPorCompetenciaECentros(
            eq(competenciaInicio), eq(competenciaFim), eq(null)))
            .thenReturn(new BigDecimal("2000.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(
            eq(competenciaInicio), eq(competenciaFim)))
            .thenReturn(5L);
        when(beneficioConsultaPort.resumoPorTipo(
            eq(competenciaInicio), eq(competenciaFim), eq(null)))
            .thenReturn(List.of());
        when(beneficioConsultaPort.matrizCentroCustoPorTipo(
            eq(competenciaInicio), eq(competenciaFim), eq(null), eq(5), eq(5)))
            .thenReturn(List.of());
        when(beneficioCustoPdfRenderer.render(any())).thenReturn("%PDF-benef".getBytes(StandardCharsets.UTF_8));

        relatorioPdfService.renderBeneficioCusto(LOGIN, USUARIO_ID, MES, ANO);

        verify(beneficioConsultaPort).somarValorPorCompetenciaECentros(
            competenciaInicio, competenciaFim, null);
    }

    private static DashboardStatsDTO statsComDados() {
        return new DashboardStatsDTO(
            150L,
            new BigDecimal("9000.00"),
            10L,
            List.of(),
            List.of(),
            List.of(),
            new BigDecimal("10000.00"),
            new BigDecimal("1000.00"),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
