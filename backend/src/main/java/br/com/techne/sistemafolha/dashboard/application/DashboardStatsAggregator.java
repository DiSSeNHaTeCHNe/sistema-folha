package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.dashboard.api.CargoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.api.LinhaNegocioStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.RubricaStatsDTO;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardStatsAggregator {

    private static final int TOP_LISTA_PADRAO = 5;

    private final FolhaConsultaPort folhaConsultaPort;
    private final FolhaTotalizacaoPort folhaTotalizacaoPort;
    private final BeneficioConsultaPort beneficioConsultaPort;

    public List<FolhaLinhaSnapshot> linhasCompetencia(
            Set<Long> centrosScoped,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean decimoTerceiro) {
        return folhaConsultaPort.findLinhasAtivasPorCompetencia(
            competenciaInicio, competenciaFim, decimoTerceiro, centrosScoped);
    }

    public long contarFuncionarios(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return folhaCompetencia.stream()
            .map(FolhaLinhaSnapshot::funcionarioId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
            .size();
    }

    public BigDecimal calcularCustoEmpresa(
            List<FolhaLinhaSnapshot> folhaCompetencia,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            AccessContextDTO contexto) {
        return folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            folhaCompetencia, competenciaInicio, competenciaFim, contexto);
    }

    public long contarBeneficiosAtivos(
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            Set<Long> centrosScoped) {
        return centrosScoped == null
            ? beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(competenciaInicio, competenciaFim)
            : beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
                competenciaInicio, competenciaFim, centrosScoped);
    }

    public BigDecimal calcularTotalProventos(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return folhaCompetencia.stream()
            .filter(fp -> "PROVENTO".equals(fp.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularTotalDescontos(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return folhaCompetencia.stream()
            .filter(fp -> "DESCONTO".equals(fp.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<CentroCustoStatsDTO> porCentroCusto(List<FolhaLinhaSnapshot> folhaCompetencia, int topN) {
        return limitarOrdenadoPorValor(calcularStatsPorCentroCusto(folhaCompetencia), topN,
            CentroCustoStatsDTO::valorTotal);
    }

    public List<LinhaNegocioStatsDTO> porLinhaNegocio(List<FolhaLinhaSnapshot> folhaCompetencia, int topN) {
        return limitarOrdenadoPorValor(calcularStatsPorLinhaNegocio(folhaCompetencia), topN,
            LinhaNegocioStatsDTO::valorTotal);
    }

    public List<CargoStatsDTO> porCargo(List<FolhaLinhaSnapshot> folhaCompetencia, int topN) {
        return limitarOrdenadoPorValor(calcularStatsPorCargo(folhaCompetencia), topN,
            CargoStatsDTO::valorTotal);
    }

    public List<RubricaStatsDTO> topProventos(List<FolhaLinhaSnapshot> folhaCompetencia, int topN) {
        return limitarOrdenadoPorValor(calcularTopProventosSemLimite(folhaCompetencia), topN,
            RubricaStatsDTO::valorTotal);
    }

    public List<RubricaStatsDTO> topDescontos(List<FolhaLinhaSnapshot> folhaCompetencia, int topN) {
        return limitarOrdenadoPorValor(calcularTopDescontosSemLimite(folhaCompetencia), topN,
            RubricaStatsDTO::valorTotal);
    }

    public DashboardStatsDTO aggregateForCompetencia(
            AccessContextDTO contexto,
            Set<Long> centrosScoped,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean decimoTerceiro) {

        List<FolhaLinhaSnapshot> folhaCompetencia = linhasCompetencia(
            centrosScoped, competenciaInicio, competenciaFim, decimoTerceiro);

        long totalFuncionarios = contarFuncionarios(folhaCompetencia);
        BigDecimal custoMensalFolha = calcularCustoEmpresa(
            folhaCompetencia, competenciaInicio, competenciaFim, contexto);
        long totalBeneficiosAtivos = contarBeneficiosAtivos(competenciaInicio, competenciaFim, centrosScoped);

        List<LinhaNegocioStatsDTO> porLinhaNegocio = calcularStatsPorLinhaNegocio(folhaCompetencia);
        List<CentroCustoStatsDTO> porCentroCusto = calcularStatsPorCentroCusto(folhaCompetencia);
        List<CargoStatsDTO> porCargo = calcularStatsPorCargo(folhaCompetencia);

        BigDecimal totalProventos = calcularTotalProventos(folhaCompetencia);
        BigDecimal totalDescontos = calcularTotalDescontos(folhaCompetencia);
        List<RubricaStatsDTO> topProventos = topProventos(folhaCompetencia, TOP_LISTA_PADRAO);
        List<RubricaStatsDTO> topDescontos = topDescontos(folhaCompetencia, TOP_LISTA_PADRAO);

        List<EvolucaoMensalDTO> evolucaoMensal = contexto.acessoTotal()
            ? calcularEvolucaoMensal()
            : calcularEvolucaoMensalScoped(centrosScoped);

        return new DashboardStatsDTO(
            totalFuncionarios,
            custoMensalFolha,
            totalBeneficiosAtivos,
            porLinhaNegocio,
            porCentroCusto,
            porCargo,
            totalProventos,
            totalDescontos,
            topProventos,
            topDescontos,
            evolucaoMensal
        );
    }

    public List<EvolucaoMensalDTO> evolucaoMeses(
            AccessContextDTO contexto,
            Set<Long> centrosScoped,
            LocalDate fimInclusive,
            int quantidadeMeses,
            boolean decimoTerceiro) {

        LocalDate fim = fimInclusive.withDayOfMonth(fimInclusive.lengthOfMonth());
        LocalDate inicio = fimInclusive.minusMonths(quantidadeMeses - 1L).withDayOfMonth(1);
        List<FolhaEvolucaoSnapshot> competencias = folhaConsultaPort.findEvolucaoUltimos12Meses(inicio);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");

        List<EvolucaoMensalDTO> resultado = new ArrayList<>();
        for (FolhaEvolucaoSnapshot item : competencias) {
            if (item.competenciaFim().isBefore(inicio) || item.competenciaInicio().isAfter(fim)) {
                continue;
            }
            if (item.decimoTerceiro() != decimoTerceiro) {
                continue;
            }
            EvolucaoMensalDTO dto = contexto.acessoTotal()
                ? toEvolucaoGlobal(item, formatter)
                : toEvolucaoScoped(item, centrosScoped, formatter);
            resultado.add(dto);
        }
        resultado.sort(Comparator.comparing(EvolucaoMensalDTO::mesAno));
        return resultado;
    }

    private EvolucaoMensalDTO toEvolucaoGlobal(FolhaEvolucaoSnapshot item, DateTimeFormatter formatter) {
        return new EvolucaoMensalDTO(
            item.competenciaInicio().format(formatter),
            item.totalLiquido(),
            item.totalEmpregados()
        );
    }

    private EvolucaoMensalDTO toEvolucaoScoped(
            FolhaEvolucaoSnapshot item, Set<Long> centros, DateTimeFormatter formatter) {
        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            item.competenciaInicio(), item.competenciaFim(), item.decimoTerceiro(), centros);
        BigDecimal custoEmpresa = folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            linhas, item.competenciaInicio(), item.competenciaFim(), null);
        int empregados = (int) linhas.stream()
            .map(FolhaLinhaSnapshot::funcionarioId)
            .filter(Objects::nonNull)
            .distinct()
            .count();
        return new EvolucaoMensalDTO(
            item.competenciaInicio().format(formatter),
            custoEmpresa,
            empregados
        );
    }

    private List<LinhaNegocioStatsDTO> calcularStatsPorLinhaNegocio(List<FolhaLinhaSnapshot> folhaCompetencia) {
        Map<Long, List<FolhaLinhaSnapshot>> porLinha = folhaCompetencia.stream()
            .filter(fp -> fp.linhaNegocioId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::linhaNegocioId));

        return porLinha.entrySet().stream()
            .map(entry -> {
                List<FolhaLinhaSnapshot> pagamentos = entry.getValue();
                long quantidadeFuncionarios = pagamentos.stream()
                    .map(FolhaLinhaSnapshot::funcionarioId)
                    .distinct()
                    .count();
                BigDecimal valorTotal = pagamentos.stream()
                    .map(FolhaLinhaSnapshot::valor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new LinhaNegocioStatsDTO(
                    entry.getKey(),
                    pagamentos.get(0).linhaNegocioDescricao(),
                    quantidadeFuncionarios,
                    valorTotal
                );
            })
            .toList();
    }

    private List<CentroCustoStatsDTO> calcularStatsPorCentroCusto(List<FolhaLinhaSnapshot> folhaCompetencia) {
        Map<Long, List<FolhaLinhaSnapshot>> porCentro = folhaCompetencia.stream()
            .filter(fp -> fp.centroCustoId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::centroCustoId));

        return porCentro.entrySet().stream()
            .map(entry -> {
                List<FolhaLinhaSnapshot> pagamentos = entry.getValue();
                long quantidadeFuncionarios = pagamentos.stream()
                    .map(FolhaLinhaSnapshot::funcionarioId)
                    .distinct()
                    .count();
                BigDecimal valorTotal = pagamentos.stream()
                    .map(FolhaLinhaSnapshot::valor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new CentroCustoStatsDTO(
                    entry.getKey(),
                    pagamentos.get(0).centroCustoDescricao(),
                    quantidadeFuncionarios,
                    valorTotal
                );
            })
            .toList();
    }

    private List<CargoStatsDTO> calcularStatsPorCargo(List<FolhaLinhaSnapshot> folhaCompetencia) {
        Map<Long, List<FolhaLinhaSnapshot>> porCargo = folhaCompetencia.stream()
            .filter(fp -> fp.cargoId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::cargoId));

        return porCargo.entrySet().stream()
            .map(entry -> {
                List<FolhaLinhaSnapshot> pagamentos = entry.getValue();
                long quantidadeFuncionarios = pagamentos.stream()
                    .map(FolhaLinhaSnapshot::funcionarioId)
                    .distinct()
                    .count();
                BigDecimal valorTotal = pagamentos.stream()
                    .map(FolhaLinhaSnapshot::valor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal valorMedio = quantidadeFuncionarios > 0
                    ? valorTotal.divide(BigDecimal.valueOf(quantidadeFuncionarios), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
                return new CargoStatsDTO(
                    entry.getKey(),
                    pagamentos.get(0).cargoDescricao(),
                    quantidadeFuncionarios,
                    valorMedio,
                    valorTotal
                );
            })
            .toList();
    }

    private List<RubricaStatsDTO> calcularTopProventosSemLimite(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return agruparRubricas(folhaCompetencia, "PROVENTO");
    }

    private List<RubricaStatsDTO> calcularTopDescontosSemLimite(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return agruparRubricas(folhaCompetencia, "DESCONTO");
    }

    private List<RubricaStatsDTO> agruparRubricas(List<FolhaLinhaSnapshot> folhaCompetencia, String tipo) {
        Map<Long, List<FolhaLinhaSnapshot>> porRubrica = folhaCompetencia.stream()
            .filter(fp -> tipo.equals(fp.tipoRubricaDescricao()))
            .filter(fp -> fp.rubricaId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::rubricaId));

        return porRubrica.entrySet().stream()
            .map(entry -> {
                List<FolhaLinhaSnapshot> linhas = entry.getValue();
                BigDecimal valorTotal = linhas.stream()
                    .map(FolhaLinhaSnapshot::valor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new RubricaStatsDTO(
                    entry.getKey(),
                    linhas.get(0).rubricaCodigo(),
                    linhas.get(0).rubricaDescricao(),
                    valorTotal,
                    (long) linhas.size()
                );
            })
            .sorted((a, b) -> b.valorTotal().compareTo(a.valorTotal()))
            .toList();
    }

    private <T> List<T> limitarOrdenadoPorValor(List<T> items, int topN, java.util.function.Function<T, BigDecimal> valorFn) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<T> ordenado = items.stream()
            .sorted((a, b) -> valorFn.apply(b).compareTo(valorFn.apply(a)))
            .toList();
        return ordenado.size() <= topN ? ordenado : ordenado.subList(0, topN);
    }

    private List<EvolucaoMensalDTO> calcularEvolucaoMensal() {
        LocalDate dataInicio = LocalDate.now(Clock.systemDefaultZone()).minusMonths(11).withDayOfMonth(1);
        List<FolhaEvolucaoSnapshot> evolucao = folhaConsultaPort.findEvolucaoUltimos12Meses(dataInicio);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");
        return evolucao.stream()
            .map(item -> new EvolucaoMensalDTO(
                item.competenciaInicio().format(formatter),
                item.totalLiquido(),
                item.totalEmpregados()
            ))
            .toList();
    }

    private List<EvolucaoMensalDTO> calcularEvolucaoMensalScoped(Set<Long> centros) {
        LocalDate dataInicio = LocalDate.now(Clock.systemDefaultZone()).minusMonths(11).withDayOfMonth(1);
        List<FolhaEvolucaoSnapshot> competencias = folhaConsultaPort.findEvolucaoUltimos12Meses(dataInicio);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");
        return competencias.stream()
            .map(item -> toEvolucaoScoped(item, centros, formatter))
            .toList();
    }
}
