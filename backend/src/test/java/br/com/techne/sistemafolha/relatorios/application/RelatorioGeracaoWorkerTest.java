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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioGeracaoWorkerTest {

    @Mock
    private RelatorioRepository relatorioRepository;
    @Mock
    private RelatorioArquivoRepository relatorioArquivoRepository;
    @Mock
    private RelatorioPdfService relatorioPdfService;
    @Mock
    private DashboardConsultaPort dashboardConsultaPort;
    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;
    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;
    @Mock
    private RelatorioRecoveryTracker recoveryTracker;

    private RelatorioGeracaoProperties properties;

    private RelatorioGeracaoWorker worker;

    private Relatorio relatorio;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        properties = new RelatorioGeracaoProperties();
        properties.setMaxTamanhoMb(50);
        worker = new RelatorioGeracaoWorker(
            relatorioRepository,
            relatorioArquivoRepository,
            relatorioPdfService,
            properties,
            dashboardConsultaPort,
            beneficioConsultaPort,
            organogramaAcessoPort,
            recoveryTracker);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("gestor@teste.com");

        relatorio = new Relatorio();
        relatorio.setId(10L);
        relatorio.setTipo(RelatorioTipo.FOLHA);
        relatorio.setMes(6);
        relatorio.setAno(2024);
        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setUsuario(usuario);
        relatorio.setAtivo(true);
    }

    @Test
    void processar_sucesso_persistePdfEAtualizaStatusProcessado() throws Exception {
        byte[] pdf = "%PDF-test".getBytes(StandardCharsets.UTF_8);
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));
        when(relatorioPdfService.renderFolhaExecutivo("gestor@teste.com", 1L, 6, 2024)).thenReturn(pdf);
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(
            new AccessContextDTO(true, true, true, null, null, 1L, "Dir", 1));
        when(dashboardConsultaPort.getStatsForCompetencia(any(), any(), any(), eq(false)))
            .thenReturn(new DashboardStatsDTO(
                100L, new BigDecimal("5000"), 5L,
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO,
                java.util.List.of(), java.util.List.of(), java.util.List.of()));
        when(beneficioConsultaPort.somarValorPorCompetenciaECentros(any(), any(), any()))
            .thenReturn(new BigDecimal("500"));

        worker.processar(10L).get();

        ArgumentCaptor<Relatorio> captor = ArgumentCaptor.forClass(Relatorio.class);
        verify(relatorioRepository).save(captor.capture());
        Relatorio salvo = captor.getValue();
        assertEquals(RelatorioStatus.PROCESSADO, salvo.getStatus());
        assertEquals(100, salvo.getTotalFuncionarios());
        assertTrue(salvo.getDataProcessamento() != null);

        ArgumentCaptor<RelatorioArquivo> arquivoCaptor = ArgumentCaptor.forClass(RelatorioArquivo.class);
        verify(relatorioArquivoRepository).save(arquivoCaptor.capture());
        assertEquals(pdf.length, arquivoCaptor.getValue().getTamanhoBytes());
        assertEquals(pdf, arquivoCaptor.getValue().getPdfBytes());
        verify(recoveryTracker).clear(10L);
    }

    @Test
    void processar_falhaRender_marcaErroTruncado() throws Exception {
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));
        when(relatorioPdfService.renderFolhaExecutivo(anyString(), anyLong(), anyInt(), anyInt()))
            .thenThrow(new IllegalStateException("x".repeat(600)));

        worker.processar(10L).get();

        ArgumentCaptor<Relatorio> captor = ArgumentCaptor.forClass(Relatorio.class);
        verify(relatorioRepository).save(captor.capture());
        Relatorio salvo = captor.getValue();
        assertEquals(RelatorioStatus.ERRO, salvo.getStatus());
        assertEquals("Erro ao gerar relatório", salvo.getErro());
        assertNotNull(salvo.getDataProcessamento());
        verify(relatorioArquivoRepository, never()).save(any());
        verify(recoveryTracker).clear(10L);
    }

    @Test
    void processar_pdfExcede50Mb_marcaErro() throws Exception {
        properties.setMaxTamanhoMb(1);
        byte[] pdfGrande = new byte[2 * 1024 * 1024];
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));
        when(relatorioPdfService.renderFolhaExecutivo(anyString(), anyLong(), anyInt(), anyInt()))
            .thenReturn(pdfGrande);

        worker.processar(10L).get();

        ArgumentCaptor<Relatorio> captor = ArgumentCaptor.forClass(Relatorio.class);
        verify(relatorioRepository).save(captor.capture());
        Relatorio salvo = captor.getValue();
        assertEquals(RelatorioStatus.ERRO, salvo.getStatus());
        assertTrue(salvo.getErro().contains("50 MB") || salvo.getErro().contains("1 MB"));
        assertNotNull(salvo.getDataProcessamento());
        verify(relatorioArquivoRepository, never()).save(any());
    }

    @Test
    void processar_idInexistente_naoSalva() throws Exception {
        when(relatorioRepository.findById(99L)).thenReturn(Optional.empty());

        worker.processar(99L).get();

        verify(relatorioRepository, never()).save(any());
        verify(recoveryTracker, never()).clear(anyLong());
    }

    @Test
    void processar_idInexistente_logaWarnNaoEncontrado() throws Exception {
        when(relatorioRepository.findById(99L)).thenReturn(Optional.empty());
        ListAppender<ILoggingEvent> appender = capturarLogsRelatorioGeracaoWorker();

        worker.processar(99L).get();

        assertTrue(appender.list.stream().anyMatch(e ->
            e.getLevel().toString().equals("WARN")
                && e.getFormattedMessage().contains("não encontrado")));
    }

    @Test
    void processar_inativoPendente_marcaErro() throws Exception {
        relatorio.setAtivo(false);
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));

        worker.processar(10L).get();

        ArgumentCaptor<Relatorio> captor = ArgumentCaptor.forClass(Relatorio.class);
        verify(relatorioRepository).save(captor.capture());
        assertEquals(RelatorioStatus.ERRO, captor.getValue().getStatus());
        assertEquals("Relatório indisponível", captor.getValue().getErro());
        assertNotNull(captor.getValue().getDataProcessamento());
        verify(recoveryTracker).clear(10L);
    }

    @Test
    void processar_sucesso_logaInicioEFinalizacao() throws Exception {
        byte[] pdf = "%PDF-test".getBytes(StandardCharsets.UTF_8);
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));
        when(relatorioPdfService.renderFolhaExecutivo("gestor@teste.com", 1L, 6, 2024)).thenReturn(pdf);
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(
            new AccessContextDTO(true, true, true, null, null, 1L, "Dir", 1));
        when(dashboardConsultaPort.getStatsForCompetencia(any(), any(), any(), eq(false)))
            .thenReturn(new DashboardStatsDTO(
                100L, new BigDecimal("5000"), 5L,
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                BigDecimal.ZERO, BigDecimal.ZERO,
                java.util.List.of(), java.util.List.of(), java.util.List.of()));
        when(beneficioConsultaPort.somarValorPorCompetenciaECentros(any(), any(), any()))
            .thenReturn(new BigDecimal("500"));
        ListAppender<ILoggingEvent> appender = capturarLogsRelatorioGeracaoWorker();

        worker.processar(10L).get();

        assertTrue(appender.list.stream().anyMatch(e ->
            e.getLevel().toString().equals("INFO")
                && e.getFormattedMessage().contains("Iniciando processamento")
                && e.getFormattedMessage().contains("id=10")
                && e.getFormattedMessage().contains("gestor@teste.com")));
        assertTrue(appender.list.stream().anyMatch(e ->
            e.getLevel().toString().equals("INFO")
                && e.getFormattedMessage().contains("processado com sucesso")
                && e.getFormattedMessage().contains("gestor@teste.com")));
    }

    @Test
    void processar_falhaRender_logaError() throws Exception {
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));
        when(relatorioPdfService.renderFolhaExecutivo(anyString(), anyLong(), anyInt(), anyInt()))
            .thenThrow(new IllegalStateException("falha render"));
        ListAppender<ILoggingEvent> appender = capturarLogsRelatorioGeracaoWorker();

        worker.processar(10L).get();

        assertTrue(appender.list.stream().anyMatch(e ->
            e.getLevel().toString().equals("ERROR")
                && e.getFormattedMessage().contains("Erro ao processar relatório 10")));
    }

    @Test
    void truncarErro_limita500Caracteres() {
        String longa = "e".repeat(600);
        assertEquals(500, RelatorioGeracaoWorker.truncarErro(longa).length());
    }

    private ListAppender<ILoggingEvent> capturarLogsRelatorioGeracaoWorker() {
        Logger logger = (Logger) LoggerFactory.getLogger(RelatorioGeracaoWorker.class);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
