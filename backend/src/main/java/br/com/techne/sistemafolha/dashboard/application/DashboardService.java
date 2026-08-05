package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final String DOMAIN = "dashboard";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);

    private final FolhaConsultaPort folhaConsultaPort;
    private final FolhaTotalizacaoPort folhaTotalizacaoPort;
    private final CadastrosImportLookupPort cadastrosImportLookupPort;
    private final BeneficioConsultaPort beneficioConsultaPort;
    private final DashboardAccessGuard dashboardAccessGuard;
    private final DashboardStatsAggregator dashboardStatsAggregator;

    public DashboardStatsDTO getStats(String login) {
        logger.debug("{}Calculando estatísticas do dashboard", DOMAIN_PREFIX);

        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
        if (access.denied()) {
            return emptyStats();
        }

        var contexto = access.contexto();
        var centrosScoped = access.centrosScoped();

        Optional<FolhaResumoSnapshot> resumoMaisRecente = folhaConsultaPort.findResumoMaisRecente();

        if (resumoMaisRecente.isEmpty()) {
            long totalFuncionarios = centrosScoped == null
                ? cadastrosImportLookupPort.countFuncionariosAtivos()
                : cadastrosImportLookupPort.countFuncionariosAtivosPorCentros(centrosScoped);

            LocalDate competenciaInicioFallback = LocalDate.now(Clock.systemDefaultZone()).withDayOfMonth(1);
            LocalDate competenciaFimFallback = competenciaInicioFallback
                .withDayOfMonth(competenciaInicioFallback.lengthOfMonth());
            long totalBeneficiosAtivos = centrosScoped == null
                ? beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(
                    competenciaInicioFallback, competenciaFimFallback)
                : beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
                    competenciaInicioFallback, competenciaFimFallback, centrosScoped);

            return new DashboardStatsDTO(
                totalFuncionarios,
                BigDecimal.ZERO,
                totalBeneficiosAtivos,
                List.of(),
                List.of(),
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of(),
                List.of(),
                List.of()
            );
        }

        FolhaResumoSnapshot resumo = resumoMaisRecente.get();
        return dashboardStatsAggregator.aggregateForCompetencia(
            contexto,
            centrosScoped,
            resumo.competenciaInicio(),
            resumo.competenciaFim(),
            resumo.decimoTerceiro()
        );
    }

    private DashboardStatsDTO emptyStats() {
        return new DashboardStatsDTO(
            0L,
            BigDecimal.ZERO,
            0L,
            List.of(),
            List.of(),
            List.of(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(),
            List.of(),
            List.of()
        );
    }
}
