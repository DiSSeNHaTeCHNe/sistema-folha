package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioCcTipoSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioFuncionarioValorSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.port.DashboardConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.relatorios.application.pdf.BeneficioCustoPdfRenderer;
import br.com.techne.sistemafolha.relatorios.application.pdf.FolhaExecutivoPdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RelatorioPdfService {

    private static final DateTimeFormatter COMPETENCIA_LABEL =
        DateTimeFormatter.ofPattern("MM/yyyy", Locale.forLanguageTag("pt-BR"));

    private final DashboardConsultaPort dashboardConsultaPort;
    private final BeneficioConsultaPort beneficioConsultaPort;
    private final FolhaTotalizacaoPort folhaTotalizacaoPort;
    private final FolhaConsultaPort folhaConsultaPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final RelatorioBrandingService brandingService;
    private final FolhaExecutivoPdfRenderer folhaExecutivoPdfRenderer;
    private final BeneficioCustoPdfRenderer beneficioCustoPdfRenderer;

    public byte[] renderFolhaExecutivo(String login, Long usuarioId, int mes, int ano) {
        LocalDate[] competencia = competenciaRange(mes, ano);
        DashboardStatsDTO stats = dashboardConsultaPort.getStatsForCompetencia(
            login, competencia[0], competencia[1], false);
        List<EvolucaoMensalDTO> evolucao = dashboardConsultaPort.getEvolucaoMeses(
            login, competencia[1], 6, false);

        boolean semDados = stats.totalFuncionarios() == null || stats.totalFuncionarios() == 0;

        RelatorioFolhaModel model = new RelatorioFolhaModel(
            brandingService.load(),
            competenciaLabel(mes, ano),
            login,
            LocalDateTime.now(),
            stats,
            evolucao,
            semDados
        );
        return folhaExecutivoPdfRenderer.render(model);
    }

    public byte[] renderBeneficioCusto(String login, Long usuarioId, int mes, int ano) {
        LocalDate[] competencia = competenciaRange(mes, ano);
        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioId);
        Set<Long> centros = contexto.acessoTotal() ? null : contexto.centrosCustoIds();

        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            competencia[0], competencia[1], false, centros);
        BigDecimal totalCustoFolha = folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            linhas, competencia[0], competencia[1], contexto);

        BigDecimal totalBeneficios = beneficioConsultaPort.somarValorPorCompetenciaECentros(
            competencia[0], competencia[1], centros);
        long qtdLancamentos = centros == null
            ? beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(competencia[0], competencia[1])
            : beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
                competencia[0], competencia[1], centros);

        List<BeneficioTipoResumoSnapshot> porTipo = beneficioConsultaPort.resumoPorTipo(
            competencia[0], competencia[1], centros);

        Map<Long, List<BeneficioFuncionarioValorSnapshot>> top10PorTipo = new HashMap<>();
        for (BeneficioTipoResumoSnapshot tipo : porTipo) {
            top10PorTipo.put(
                tipo.tipoBeneficioId(),
                beneficioConsultaPort.topFuncionariosPorTipo(
                    tipo.tipoBeneficioId(), competencia[0], competencia[1], centros, 10));
        }

        List<BeneficioCcTipoSnapshot> matriz = beneficioConsultaPort.matrizCentroCustoPorTipo(
            competencia[0], competencia[1], centros, 5, 5);

        boolean semBeneficios = porTipo.isEmpty() && qtdLancamentos == 0;
        boolean semFolha = linhas.isEmpty();

        BigDecimal custoConsolidado = totalCustoFolha.add(
            totalBeneficios != null ? totalBeneficios : BigDecimal.ZERO);

        RelatorioBeneficioModel model = new RelatorioBeneficioModel(
            brandingService.load(),
            competenciaLabel(mes, ano),
            login,
            LocalDateTime.now(),
            totalBeneficios != null ? totalBeneficios : BigDecimal.ZERO,
            qtdLancamentos,
            totalCustoFolha,
            custoConsolidado,
            porTipo,
            top10PorTipo,
            matriz,
            semBeneficios,
            semFolha
        );
        return beneficioCustoPdfRenderer.render(model);
    }

    private static LocalDate[] competenciaRange(int mes, int ano) {
        YearMonth ym = YearMonth.of(ano, mes);
        return new LocalDate[] { ym.atDay(1), ym.atEndOfMonth() };
    }

    private static String competenciaLabel(int mes, int ano) {
        return YearMonth.of(ano, mes).format(COMPETENCIA_LABEL);
    }
}
