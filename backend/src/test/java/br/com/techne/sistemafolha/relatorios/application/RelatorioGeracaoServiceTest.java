package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.relatorios.api.RelatorioFolhaDTO;
import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioAcessoNegadoException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioArquivo;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioGeracaoLimiteException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioIndisponivelException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioNotFoundException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioArquivoRepository;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioGeracaoServiceTest {

    private static final String LOGIN = "gestor@teste.com";

    @Mock
    private RelatorioRepository relatorioRepository;
    @Mock
    private RelatorioArquivoRepository relatorioArquivoRepository;
    @Mock
    private RelatorioGeracaoWorker relatorioGeracaoWorker;
    @Mock
    private UsuarioLookupPort usuarioLookupPort;
    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;
    @Mock
    private PlatformTransactionManager transactionManager;

    private RelatorioGeracaoProperties properties;

    private RelatorioGeracaoService service;

    private Usuario usuario;
    private Relatorio relatorio;

    @BeforeEach
    void setUp() {
        properties = new RelatorioGeracaoProperties();
        properties.setTimeoutSegundos(60);
        properties.setMaxJobsSimultaneosPorUsuario(3);

        TransactionStatus txStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(txStatus);
        doAnswer(invocation -> {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                    sync.afterCommit();
                }
                TransactionSynchronizationManager.clearSynchronization();
            }
            return null;
        }).when(transactionManager).commit(any());

        service = new RelatorioGeracaoService(
            relatorioRepository,
            relatorioArquivoRepository,
            relatorioGeracaoWorker,
            properties,
            usuarioLookupPort,
            organogramaAcessoPort,
            transactionManager);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin(LOGIN);

        relatorio = new Relatorio();
        relatorio.setId(10L);
        relatorio.setTipo(RelatorioTipo.FOLHA);
        relatorio.setMes(6);
        relatorio.setAno(YearMonth.now().getYear());
        relatorio.setUsuario(usuario);
        relatorio.setAtivo(true);
    }

    @Test
    void gerarFolha_competenciaFutura_lancaIllegalArgumentException() {
        YearMonth futura = YearMonth.now().plusMonths(1);
        assertThrows(IllegalArgumentException.class,
            () -> service.gerarFolha(LOGIN, futura.getMonthValue(), futura.getYear()));
    }

    @Test
    void gerarFolha_aclNegado_lancaRelatorioAcessoNegadoException() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(
            new AccessContextDTO(false, false, false, Set.of(), MotivoNegacaoAcesso.SEM_FUNCIONARIO,
                null, null, null));

        assertThrows(RelatorioAcessoNegadoException.class,
            () -> service.gerarFolha(LOGIN, 1, YearMonth.now().getYear()));
    }

    @Test
    void gerarFolha_limitePendente_lancaRelatorioGeracaoLimiteException() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(
            new AccessContextDTO(true, true, true, null, null, 1L, "Dir", 1));
        when(relatorioRepository.findByUsuarioIdAndTipoAndMesAndAnoAndAtivoTrue(
            eq(1L), eq(RelatorioTipo.FOLHA), anyInt(), anyInt()))
            .thenReturn(Optional.empty());
        when(relatorioRepository.countByUsuarioIdAndStatusAndAtivoTrue(1L, RelatorioStatus.PENDENTE))
            .thenReturn(3L);

        assertThrows(RelatorioGeracaoLimiteException.class,
            () -> service.gerarFolha(LOGIN, 1, YearMonth.now().getYear()));
    }

    @Test
    void gerarFolha_mesmaTupla_substituiRegistroAnterior() {
        int mes = 1;
        int ano = YearMonth.now().getYear();
        relatorio.setMes(mes);
        relatorio.setStatus(RelatorioStatus.PROCESSADO);

        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(
            new AccessContextDTO(true, true, true, null, null, 1L, "Dir", 1));
        when(relatorioRepository.findByUsuarioIdAndTipoAndMesAndAnoAndAtivoTrue(
            1L, RelatorioTipo.FOLHA, mes, ano)).thenReturn(Optional.of(relatorio));
        when(relatorioRepository.countByUsuarioIdAndStatusAndAtivoTrue(1L, RelatorioStatus.PENDENTE))
            .thenReturn(0L);
        when(relatorioRepository.save(any())).thenAnswer(invocation -> {
            Relatorio salvo = invocation.getArgument(0);
            assertEquals(RelatorioStatus.PENDENTE, salvo.getStatus());
            return salvo;
        });
        when(relatorioGeracaoWorker.processar(10L)).thenAnswer(invocation -> {
            relatorio.setStatus(RelatorioStatus.PROCESSADO);
            return CompletableFuture.completedFuture(null);
        });
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));

        RelatorioFolhaDTO dto = service.gerarFolha(LOGIN, mes, ano);

        assertEquals(RelatorioStatus.PROCESSADO, dto.status());
        verify(relatorioArquivoRepository).findByRelatorioId(10L);
        verify(relatorioRepository).save(relatorio);
    }

    @Test
    void downloadPdf_processado_retornaBytes() {
        relatorio.setStatus(RelatorioStatus.PROCESSADO);
        byte[] pdf = "%PDF".getBytes(StandardCharsets.UTF_8);
        RelatorioArquivo arquivo = new RelatorioArquivo();
        arquivo.setPdfBytes(pdf);

        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(relatorioRepository.findByIdAndUsuarioIdAndAtivoTrue(10L, 1L))
            .thenReturn(Optional.of(relatorio));
        when(relatorioArquivoRepository.findByRelatorioId(10L)).thenReturn(Optional.of(arquivo));

        byte[] result = service.downloadPdf(LOGIN, 10L, RelatorioTipo.FOLHA);

        assertArrayEquals(pdf, result);
    }

    @Test
    void downloadPdf_pendente_lancaRelatorioIndisponivelException() {
        relatorio.setStatus(RelatorioStatus.PENDENTE);

        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(relatorioRepository.findByIdAndUsuarioIdAndAtivoTrue(10L, 1L))
            .thenReturn(Optional.of(relatorio));

        assertThrows(RelatorioIndisponivelException.class,
            () -> service.downloadPdf(LOGIN, 10L, RelatorioTipo.FOLHA));
        verify(relatorioArquivoRepository, never()).findByRelatorioId(any());
    }

    @Test
    void downloadPdf_naoEncontrado_lancaRelatorioNotFoundException() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(relatorioRepository.findByIdAndUsuarioIdAndAtivoTrue(99L, 1L))
            .thenReturn(Optional.empty());

        assertThrows(RelatorioNotFoundException.class,
            () -> service.downloadPdf(LOGIN, 99L, RelatorioTipo.FOLHA));
    }

    @Test
    void listarFolha_usaOrdenacaoRepositorioAnoMesDesc() {
        Relatorio antigo = new Relatorio();
        antigo.setId(1L);
        antigo.setTipo(RelatorioTipo.FOLHA);
        antigo.setMes(1);
        antigo.setAno(2024);
        antigo.setStatus(RelatorioStatus.PROCESSADO);
        antigo.setAtivo(true);

        Relatorio recente = new Relatorio();
        recente.setId(2L);
        recente.setTipo(RelatorioTipo.FOLHA);
        recente.setMes(6);
        recente.setAno(2026);
        recente.setStatus(RelatorioStatus.PROCESSADO);
        recente.setAtivo(true);

        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(relatorioRepository.findByTipoAndAtivoTrueOrderByAnoDescMesDesc(RelatorioTipo.FOLHA))
            .thenReturn(List.of(recente, antigo));

        List<RelatorioFolhaDTO> resultado = service.listarFolha(LOGIN);

        assertEquals(2, resultado.size());
        assertEquals(2026, resultado.get(0).ano());
        assertEquals(6, resultado.get(0).mes());
        assertEquals(2024, resultado.get(1).ano());
        assertEquals(1, resultado.get(1).mes());
        verify(relatorioRepository).findByTipoAndAtivoTrueOrderByAnoDescMesDesc(RelatorioTipo.FOLHA);
    }

    @Test
    void gerarFolha_disparaWorkerAposCommit() {
        int mes = 1;
        int ano = YearMonth.now().getYear();
        relatorio.setMes(mes);

        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(
            new AccessContextDTO(true, true, true, null, null, 1L, "Dir", 1));
        when(relatorioRepository.findByUsuarioIdAndTipoAndMesAndAnoAndAtivoTrue(
            1L, RelatorioTipo.FOLHA, mes, ano)).thenReturn(Optional.empty());
        when(relatorioRepository.countByUsuarioIdAndStatusAndAtivoTrue(1L, RelatorioStatus.PENDENTE))
            .thenReturn(0L);
        when(relatorioRepository.save(any())).thenAnswer(invocation -> {
            Relatorio salvo = invocation.getArgument(0);
            salvo.setId(10L);
            return salvo;
        });
        when(relatorioGeracaoWorker.processar(10L)).thenAnswer(invocation -> {
            relatorio.setStatus(RelatorioStatus.PROCESSADO);
            return CompletableFuture.completedFuture(null);
        });
        when(relatorioRepository.findById(10L)).thenReturn(Optional.of(relatorio));

        service.gerarFolha(LOGIN, mes, ano);

        verify(relatorioGeracaoWorker).processar(10L);
    }
}
