package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioArquivoRepository;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioStaleRecoveryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant FIXED = Instant.parse("2026-06-15T12:00:00Z");

    @Mock
    private RelatorioRepository relatorioRepository;
    @Mock
    private RelatorioArquivoRepository relatorioArquivoRepository;
    @Mock
    private RelatorioRecoveryTracker recoveryTracker;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private Consumer<Long> enqueueFn;

    private RelatorioStaleDetector staleDetector;
    private RelatorioStaleRecoveryService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        RelatorioGeracaoProperties properties = new RelatorioGeracaoProperties();
        properties.setTimeoutSegundos(60);
        properties.setStaleGraceSegundos(120);
        Clock clock = Clock.fixed(FIXED, ZONE);
        staleDetector = new RelatorioStaleDetector(properties, clock);

        service = new RelatorioStaleRecoveryService(
            relatorioRepository,
            relatorioArquivoRepository,
            staleDetector,
            recoveryTracker,
            enqueueFn,
            transactionManager);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("gestor@teste.com");
    }

    @Test
    void recuperarRelatorio_primeiraDetecaoStale_reenfileiraEMarcaAttempted() {
        Relatorio relatorio = stalePendente(10L);
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());
        when(recoveryTracker.hasAttempted(10L)).thenReturn(false);

        service.recuperarRelatorio(relatorio);

        verify(recoveryTracker).markAttempted(10L);
        verify(enqueueFn).accept(10L);
        verify(relatorioRepository, never()).save(any());
    }

    @Test
    void recuperarRelatorio_segundaDetecaoStale_promoveErroTempoEsgotado() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        Relatorio relatorio = stalePendente(10L);
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());
        when(recoveryTracker.hasAttempted(10L)).thenReturn(true);

        service.recuperarRelatorio(relatorio);

        ArgumentCaptor<Relatorio> captor = ArgumentCaptor.forClass(Relatorio.class);
        verify(relatorioRepository).save(captor.capture());
        assertEquals(RelatorioStatus.ERRO, captor.getValue().getStatus());
        assertEquals(RelatorioGeracaoConstants.ERRO_TEMPO_ESGOTADO, captor.getValue().getErro());
        verify(recoveryTracker).clear(10L);
        verify(enqueueFn, never()).accept(any());
    }

    @Test
    void recuperarRelatorio_naoStale_naoFazNada() {
        Relatorio relatorio = recentPendente(10L);
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());

        service.recuperarRelatorio(relatorio);

        verify(enqueueFn, never()).accept(any());
        verify(relatorioRepository, never()).save(any());
        verify(recoveryTracker, never()).markAttempted(any());
    }

    @Test
    void recuperarParaUsuario_processaTodosPendentesDoUsuario() {
        Relatorio stale1 = stalePendente(10L);
        Relatorio stale2 = stalePendente(11L);
        when(relatorioRepository.findByUsuarioIdAndStatusAndAtivoTrue(1L, RelatorioStatus.PENDENTE))
            .thenReturn(List.of(stale1, stale2));
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());
        when(relatorioArquivoRepository.findByRelatorioId(11L)).thenReturn(Optional.empty());
        when(recoveryTracker.hasAttempted(any())).thenReturn(false);

        service.recuperarParaUsuario(1L);

        verify(enqueueFn).accept(10L);
        verify(enqueueFn).accept(11L);
    }

    @Test
    void contarPendentesAtivos_excluiStale() {
        Relatorio stale = stalePendente(10L);
        Relatorio recente = recentPendente(11L);
        when(relatorioRepository.findByUsuarioIdAndStatusAndAtivoTrue(1L, RelatorioStatus.PENDENTE))
            .thenReturn(List.of(stale, recente));
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());
        when(relatorioArquivoRepository.findByRelatorioId(11L)).thenReturn(Optional.empty());

        long count = service.contarPendentesAtivos(1L);

        assertEquals(1L, count);
    }

    @Test
    void contarPendentesAtivos_excluiPromovidosErro() {
        Relatorio stale = stalePendente(10L);
        when(relatorioRepository.findByUsuarioIdAndStatusAndAtivoTrue(1L, RelatorioStatus.PENDENTE))
            .thenReturn(List.of(stale));
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.empty());

        long count = service.contarPendentesAtivos(1L);

        assertEquals(0L, count);
    }

    private Relatorio stalePendente(Long id) {
        Relatorio relatorio = basePendente(id);
        relatorio.setDataCriacao(LocalDateTime.ofInstant(FIXED, ZONE).minusSeconds(200));
        return relatorio;
    }

    private Relatorio recentPendente(Long id) {
        Relatorio relatorio = basePendente(id);
        relatorio.setDataCriacao(LocalDateTime.ofInstant(FIXED, ZONE).minusSeconds(30));
        return relatorio;
    }

    private Relatorio basePendente(Long id) {
        Relatorio relatorio = new Relatorio();
        relatorio.setId(id);
        relatorio.setTipo(RelatorioTipo.FOLHA);
        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setUsuario(usuario);
        relatorio.setAtivo(true);
        return relatorio;
    }
}
