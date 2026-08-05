package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.dashboard.application.DashboardStatsAggregator;
import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrcamentoConsultaAdapter implements OrcamentoConsultaPort {

    private final FolhaConsultaPort folhaConsultaPort;
    private final DashboardStatsAggregator dashboardStatsAggregator;

    @Override
    public List<OrcamentoCentroCustoDTO> obterRealizadoPorCentroCusto(
            AccessContextDTO ctx, YearMonth competencia) {
        if (semEscopo(ctx)) {
            return List.of();
        }
        LocalDate inicio = competencia.atDay(1);
        LocalDate fim = competencia.atEndOfMonth();
        if (!folhaConsultaPort.existsResumoAtivo(inicio, fim, false)) {
            return List.of();
        }
        Set<Long> centros = ctx.acessoTotal() ? null : ctx.centrosCustoIds();
        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            inicio, fim, false, centros);
        return dashboardStatsAggregator.porCentroCusto(linhas, Integer.MAX_VALUE).stream()
            .map(this::toCentroCustoDto)
            .toList();
    }

    @Override
    public List<OrcamentoNodeDTO> consolidarHierarquia(AccessContextDTO ctx, YearMonth competencia) {
        return obterRealizadoPorCentroCusto(ctx, competencia).stream()
            .map(cc -> new OrcamentoNodeDTO(
                cc.centroCustoId(),
                cc.centroCustoDescricao(),
                cc.realizado(),
                List.of()))
            .toList();
    }

    private OrcamentoCentroCustoDTO toCentroCustoDto(CentroCustoStatsDTO stats) {
        return new OrcamentoCentroCustoDTO(
            stats.id(),
            stats.descricao(),
            stats.valorTotal(),
            stats.quantidadeFuncionarios());
    }

    private boolean semEscopo(AccessContextDTO ctx) {
        if (ctx == null) {
            return true;
        }
        if (ctx.motivoNegacao() != null) {
            return true;
        }
        if (ctx.acessoTotal()) {
            return false;
        }
        return ctx.centrosCustoIds() == null || ctx.centrosCustoIds().isEmpty();
    }
}
