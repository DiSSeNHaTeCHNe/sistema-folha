package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        return aggregator().aggregateForCompetencia(
            contexto,
            centrosScoped,
            resumo.competenciaInicio(),
            resumo.competenciaFim(),
            resumo.decimoTerceiro()
        );
    }

    private DashboardStatsAggregator aggregator() {
        return new DashboardStatsAggregator(folhaConsultaPort, folhaTotalizacaoPort, beneficioConsultaPort);
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
}
