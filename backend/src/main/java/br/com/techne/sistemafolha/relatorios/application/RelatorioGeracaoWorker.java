package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.port.DashboardConsultaPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioArquivo;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioArquivoRepository;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelatorioGeracaoWorker {

    private static final String ERRO_TAMANHO =
        "PDF excede o tamanho máximo permitido (%d MB)";

    private final RelatorioRepository relatorioRepository;
    private final RelatorioArquivoRepository relatorioArquivoRepository;
    private final RelatorioPdfService relatorioPdfService;
    private final RelatorioGeracaoProperties properties;
    private final DashboardConsultaPort dashboardConsultaPort;
    private final BeneficioConsultaPort beneficioConsultaPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    @Async("relatorioExecutor")
    @Transactional
    public CompletableFuture<Void> processar(Long relatorioId) {
        Relatorio relatorio = relatorioRepository.findById(relatorioId).orElse(null);
        if (relatorio == null || !Boolean.TRUE.equals(relatorio.getAtivo())) {
            log.warn("Relatório {} não encontrado ou inativo", relatorioId);
            return CompletableFuture.completedFuture(null);
        }

        Usuario usuario = relatorio.getUsuario();
        String login = usuario.getLogin();
        Long usuarioId = usuario.getId();
        int mes = relatorio.getMes();
        int ano = relatorio.getAno();

        try {
            byte[] pdf = renderPdf(relatorio.getTipo(), login, usuarioId, mes, ano);
            long maxBytes = (long) properties.getMaxTamanhoMb() * 1024 * 1024;
            if (pdf.length > maxBytes) {
                marcarErro(relatorio, ERRO_TAMANHO.formatted(properties.getMaxTamanhoMb()));
                return CompletableFuture.completedFuture(null);
            }

            persistirArquivo(relatorio, pdf);
            preencherTotais(relatorio, login, usuarioId, mes, ano);
            relatorio.setStatus(RelatorioStatus.PROCESSADO);
            relatorio.setDataProcessamento(LocalDateTime.now());
            relatorio.setErro(null);
            relatorioRepository.save(relatorio);
            log.info("Relatório {} processado com sucesso login={} competencia={}/{}",
                relatorioId, login, mes, ano);
        } catch (Exception e) {
            log.error("Erro ao processar relatório {} login={}", relatorioId, login, e);
            marcarErro(relatorio, "Erro ao gerar relatório");
        }

        return CompletableFuture.completedFuture(null);
    }

    private byte[] renderPdf(RelatorioTipo tipo, String login, Long usuarioId, int mes, int ano) {
        return switch (tipo) {
            case FOLHA -> relatorioPdfService.renderFolhaExecutivo(login, usuarioId, mes, ano);
            case BENEFICIO -> relatorioPdfService.renderBeneficioCusto(login, usuarioId, mes, ano);
        };
    }

    private void persistirArquivo(Relatorio relatorio, byte[] pdf) {
        relatorioArquivoRepository.findByRelatorioId(relatorio.getId())
            .ifPresent(relatorioArquivoRepository::delete);

        RelatorioArquivo arquivo = new RelatorioArquivo();
        arquivo.setRelatorio(relatorio);
        arquivo.setPdfBytes(pdf);
        arquivo.setTamanhoBytes((long) pdf.length);
        relatorioArquivoRepository.save(arquivo);
    }

    private void preencherTotais(Relatorio relatorio, String login, Long usuarioId, int mes, int ano) {
        YearMonth ym = YearMonth.of(ano, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();
        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioId);
        Set<Long> centros = contexto.acessoTotal() ? null : contexto.centrosCustoIds();

        if (relatorio.getTipo() == RelatorioTipo.FOLHA) {
            DashboardStatsDTO stats = dashboardConsultaPort.getStatsForCompetencia(
                login, inicio, fim, false);
            relatorio.setTotalFuncionarios(
                stats.totalFuncionarios() != null ? stats.totalFuncionarios().intValue() : 0);
            relatorio.setTotalFolha(stats.custoMensalFolha());
            BigDecimal totalBeneficios = beneficioConsultaPort.somarValorPorCompetenciaECentros(
                inicio, fim, centros);
            relatorio.setTotalBeneficios(totalBeneficios != null ? totalBeneficios : BigDecimal.ZERO);
            relatorio.setTotalValor(stats.custoMensalFolha());
        } else {
            BigDecimal totalBeneficios = beneficioConsultaPort.somarValorPorCompetenciaECentros(
                inicio, fim, centros);
            DashboardStatsDTO stats = dashboardConsultaPort.getStatsForCompetencia(
                login, inicio, fim, false);
            BigDecimal custoFolha = stats.custoMensalFolha() != null
                ? stats.custoMensalFolha() : BigDecimal.ZERO;
            BigDecimal benef = totalBeneficios != null ? totalBeneficios : BigDecimal.ZERO;
            relatorio.setTotalBeneficios(benef);
            relatorio.setTotalFolha(custoFolha);
            relatorio.setTotalValor(custoFolha.add(benef));
        }
    }

    private void marcarErro(Relatorio relatorio, String mensagem) {
        relatorio.setStatus(RelatorioStatus.ERRO);
        relatorio.setDataProcessamento(LocalDateTime.now());
        relatorio.setErro(truncarErro(mensagem));
        relatorioRepository.save(relatorio);
    }

    static String truncarErro(String mensagem) {
        if (mensagem == null) {
            return "Erro ao gerar relatório";
        }
        return mensagem.length() <= 500 ? mensagem : mensagem.substring(0, 500);
    }
}
