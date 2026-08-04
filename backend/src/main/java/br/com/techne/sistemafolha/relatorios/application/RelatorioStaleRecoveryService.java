package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioArquivoRepository;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class RelatorioStaleRecoveryService {

    private final RelatorioRepository relatorioRepository;
    private final RelatorioArquivoRepository relatorioArquivoRepository;
    private final RelatorioStaleDetector staleDetector;
    private final RelatorioRecoveryTracker recoveryTracker;
    private final Consumer<Long> enqueueFn;
    private final TransactionTemplate transactionTemplate;

    public RelatorioStaleRecoveryService(
            RelatorioRepository relatorioRepository,
            RelatorioArquivoRepository relatorioArquivoRepository,
            RelatorioStaleDetector staleDetector,
            RelatorioRecoveryTracker recoveryTracker,
            Consumer<Long> relatorioEnqueueFn,
            PlatformTransactionManager transactionManager) {
        this.relatorioRepository = relatorioRepository;
        this.relatorioArquivoRepository = relatorioArquivoRepository;
        this.staleDetector = staleDetector;
        this.recoveryTracker = recoveryTracker;
        this.enqueueFn = relatorioEnqueueFn;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void recuperarParaUsuario(Long usuarioId) {
        List<Relatorio> pendentes = relatorioRepository.findByUsuarioIdAndStatusAndAtivoTrue(
            usuarioId, RelatorioStatus.PENDENTE);
        for (Relatorio relatorio : pendentes) {
            recuperarRelatorio(relatorio);
        }
    }

    public void recuperarRelatorio(Relatorio relatorio) {
        if (relatorio.getStatus() != RelatorioStatus.PENDENTE) {
            return;
        }
        boolean hasBlob = relatorioArquivoRepository.findByRelatorioId(relatorio.getId()).isPresent();
        if (!staleDetector.isStale(relatorio, hasBlob)) {
            return;
        }
        Long relatorioId = relatorio.getId();
        if (!recoveryTracker.hasAttempted(relatorioId)) {
            log.info("Recovery stale: reenfileirando relatório id={} login={}",
                relatorioId, relatorio.getUsuario().getLogin());
            recoveryTracker.markAttempted(relatorioId);
            enqueueFn.accept(relatorioId);
        } else {
            log.warn("Recovery stale: promovendo relatório id={} a ERRO tempo esgotado", relatorioId);
            transactionTemplate.executeWithoutResult(status -> promoverErroTempoEsgotado(relatorio));
        }
    }

    public long contarPendentesAtivos(Long usuarioId) {
        return relatorioRepository.findByUsuarioIdAndStatusAndAtivoTrue(usuarioId, RelatorioStatus.PENDENTE)
            .stream()
            .filter(r -> {
                boolean hasBlob = relatorioArquivoRepository.findByRelatorioId(r.getId()).isPresent();
                return !staleDetector.isStale(r, hasBlob);
            })
            .count();
    }

    private void promoverErroTempoEsgotado(Relatorio relatorio) {
        relatorio.setStatus(RelatorioStatus.ERRO);
        relatorio.setErro(RelatorioGeracaoConstants.ERRO_TEMPO_ESGOTADO);
        relatorio.setDataProcessamento(LocalDateTime.now());
        relatorioRepository.save(relatorio);
        recoveryTracker.clear(relatorio.getId());
    }
}
