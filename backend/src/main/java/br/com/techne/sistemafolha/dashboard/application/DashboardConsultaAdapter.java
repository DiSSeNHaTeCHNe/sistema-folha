package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.port.DashboardConsultaPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardConsultaAdapter implements DashboardConsultaPort {

    private final DashboardStatsAggregator statsAggregator;
    private final DashboardAccessGuard dashboardAccessGuard;

    @Override
    public DashboardStatsDTO getStatsForCompetencia(
            String login, LocalDate competenciaInicio, LocalDate competenciaFim, boolean decimoTerceiro) {
        ResolvedAccess access = resolveAccess(login);
        if (access.isDenied()) {
            return emptyStats();
        }
        return statsAggregator.aggregateForCompetencia(
            access.contexto(), access.centrosScoped(), competenciaInicio, competenciaFim, decimoTerceiro);
    }

    @Override
    public List<EvolucaoMensalDTO> getEvolucaoMeses(
            String login, LocalDate fimInclusive, int quantidadeMeses, boolean decimoTerceiro) {
        ResolvedAccess access = resolveAccess(login);
        if (access.isDenied()) {
            return List.of();
        }
        return statsAggregator.evolucaoMeses(
            access.contexto(), access.centrosScoped(), fimInclusive, quantidadeMeses, decimoTerceiro);
    }

    private ResolvedAccess resolveAccess(String login) {
        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
        if (access.denied()) {
            return ResolvedAccess.deniedAccess();
        }
        return new ResolvedAccess(false, access.contexto(), access.centrosScoped());
    }

    private DashboardStatsDTO emptyStats() {
        return new DashboardStatsDTO(
            0L, java.math.BigDecimal.ZERO, 0L,
            List.of(), List.of(), List.of(),
            java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
            List.of(), List.of(), List.of()
        );
    }

    private record ResolvedAccess(boolean denied, AccessContextDTO contexto, Set<Long> centrosScoped) {
        boolean isDenied() {
            return denied;
        }

        static ResolvedAccess deniedAccess() {
            return new ResolvedAccess(true, null, null);
        }
    }
}
