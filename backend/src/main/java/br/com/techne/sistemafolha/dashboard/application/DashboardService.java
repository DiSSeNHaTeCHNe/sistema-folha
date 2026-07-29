package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.dashboard.api.CargoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.dashboard.api.LinhaNegocioStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.RubricaStatsDTO;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final UsuarioLookupPort usuarioLookupPort;

    public DashboardStatsDTO getStats(String login) {
        logger.debug("{}Calculando estatísticas do dashboard", DOMAIN_PREFIX);

        if (login == null || login.isBlank()) {
            return emptyStats();
        }

        Optional<Usuario> usuarioOpt = usuarioLookupPort.findByLoginAndAtivoTrue(login);
        if (usuarioOpt.isEmpty()) {
            return emptyStats();
        }

        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioOpt.get().getId());
        if (deveNegarAcesso(contexto)) {
            return emptyStats();
        }

        Set<Long> centrosScoped = contexto.acessoTotal() ? null : contexto.centrosCustoIds();

        Optional<FolhaResumoSnapshot> resumoMaisRecente = folhaConsultaPort.findResumoMaisRecente();

        if (resumoMaisRecente.isEmpty()) {
            long totalFuncionarios = centrosScoped == null
                ? cadastrosImportLookupPort.countFuncionariosAtivos()
                : cadastrosImportLookupPort.countFuncionariosAtivosPorCentros(centrosScoped);

            LocalDate competenciaInicioFallback = LocalDate.now().withDayOfMonth(1);
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
        LocalDate competenciaInicio = resumo.competenciaInicio();
        LocalDate competenciaFim = resumo.competenciaFim();

        List<FolhaLinhaSnapshot> folhaCompetencia = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            competenciaInicio, competenciaFim, resumo.decimoTerceiro(), centrosScoped);

        long totalFuncionarios = folhaCompetencia.stream()
            .map(FolhaLinhaSnapshot::funcionarioId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
            .size();

        BigDecimal custoMensalFolha = folhaTotalizacaoPort.calcularTotalCustoEmpresa(
            folhaCompetencia, competenciaInicio, competenciaFim, contexto);

        long totalBeneficiosAtivos = centrosScoped == null
            ? beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(competenciaInicio, competenciaFim)
            : beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
                competenciaInicio, competenciaFim, centrosScoped);

        List<LinhaNegocioStatsDTO> porLinhaNegocio = calcularStatsPorLinhaNegocio(folhaCompetencia);
        List<CentroCustoStatsDTO> porCentroCusto = calcularStatsPorCentroCusto(folhaCompetencia);
        List<CargoStatsDTO> porCargo = calcularStatsPorCargo(folhaCompetencia);

        BigDecimal totalProventos = calcularTotalProventos(folhaCompetencia);
        BigDecimal totalDescontos = calcularTotalDescontos(folhaCompetencia);
        List<RubricaStatsDTO> topProventos = calcularTopProventos(folhaCompetencia);
        List<RubricaStatsDTO> topDescontos = calcularTopDescontos(folhaCompetencia);

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

    private boolean deveNegarAcesso(AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return false;
        }
        if (contexto.motivoNegacao() != null) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return true;
        }
        return contexto.centrosCustoIds() == null || contexto.centrosCustoIds().isEmpty();
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

    private BigDecimal calcularTotalProventos(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return folhaCompetencia.stream()
            .filter(fp -> "PROVENTO".equals(fp.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularTotalDescontos(List<FolhaLinhaSnapshot> folhaCompetencia) {
        return folhaCompetencia.stream()
            .filter(fp -> "DESCONTO".equals(fp.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RubricaStatsDTO> calcularTopProventos(List<FolhaLinhaSnapshot> folhaCompetencia) {
        Map<Long, List<FolhaLinhaSnapshot>> porRubrica = folhaCompetencia.stream()
            .filter(fp -> "PROVENTO".equals(fp.tipoRubricaDescricao()))
            .filter(fp -> fp.rubricaId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::rubricaId));

        return porRubrica.entrySet().stream()
            .map(entry -> {
                List<FolhaLinhaSnapshot> proventos = entry.getValue();
                BigDecimal valorTotal = proventos.stream()
                    .map(FolhaLinhaSnapshot::valor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new RubricaStatsDTO(
                    entry.getKey(),
                    proventos.get(0).rubricaCodigo(),
                    proventos.get(0).rubricaDescricao(),
                    valorTotal,
                    (long) proventos.size()
                );
            })
            .sorted((a, b) -> b.valorTotal().compareTo(a.valorTotal()))
            .limit(5)
            .toList();
    }

    private List<RubricaStatsDTO> calcularTopDescontos(List<FolhaLinhaSnapshot> folhaCompetencia) {
        Map<Long, List<FolhaLinhaSnapshot>> porRubrica = folhaCompetencia.stream()
            .filter(fp -> "DESCONTO".equals(fp.tipoRubricaDescricao()))
            .filter(fp -> fp.rubricaId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::rubricaId));

        return porRubrica.entrySet().stream()
            .map(entry -> {
                List<FolhaLinhaSnapshot> descontos = entry.getValue();
                BigDecimal valorTotal = descontos.stream()
                    .map(FolhaLinhaSnapshot::valor)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                return new RubricaStatsDTO(
                    entry.getKey(),
                    descontos.get(0).rubricaCodigo(),
                    descontos.get(0).rubricaDescricao(),
                    valorTotal,
                    (long) descontos.size()
                );
            })
            .sorted((a, b) -> b.valorTotal().compareTo(a.valorTotal()))
            .limit(5)
            .toList();
    }

    private List<EvolucaoMensalDTO> calcularEvolucaoMensal() {
        LocalDate dataInicio = LocalDate.now().minusMonths(11).withDayOfMonth(1);
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
        LocalDate dataInicio = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        List<FolhaEvolucaoSnapshot> competencias = folhaConsultaPort.findEvolucaoUltimos12Meses(dataInicio);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yyyy");
        return competencias.stream()
            .map(item -> {
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
            })
            .toList();
    }
}
