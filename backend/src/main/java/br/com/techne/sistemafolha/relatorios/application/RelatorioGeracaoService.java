package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.relatorios.api.RelatorioBeneficioDTO;
import br.com.techne.sistemafolha.relatorios.api.RelatorioFolhaDTO;
import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioAcessoNegadoException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioGeracaoLimiteException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioIndisponivelException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioNotFoundException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioArquivoRepository;
import br.com.techne.sistemafolha.relatorios.infrastructure.RelatorioRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RelatorioGeracaoService {

    private final RelatorioRepository relatorioRepository;
    private final RelatorioArquivoRepository relatorioArquivoRepository;
    private final RelatorioGeracaoWorker relatorioGeracaoWorker;
    private final RelatorioGeracaoProperties properties;
    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final RelatorioStaleRecoveryService staleRecoveryService;
    private final RelatorioStaleDetector staleDetector;
    private final TransactionTemplate transactionTemplate;

    public RelatorioGeracaoService(
            RelatorioRepository relatorioRepository,
            RelatorioArquivoRepository relatorioArquivoRepository,
            RelatorioGeracaoWorker relatorioGeracaoWorker,
            RelatorioGeracaoProperties properties,
            UsuarioLookupPort usuarioLookupPort,
            OrganogramaAcessoPort organogramaAcessoPort,
            @Lazy RelatorioStaleRecoveryService staleRecoveryService,
            RelatorioStaleDetector staleDetector,
            PlatformTransactionManager transactionManager) {
        this.relatorioRepository = relatorioRepository;
        this.relatorioArquivoRepository = relatorioArquivoRepository;
        this.relatorioGeracaoWorker = relatorioGeracaoWorker;
        this.properties = properties;
        this.usuarioLookupPort = usuarioLookupPort;
        this.organogramaAcessoPort = organogramaAcessoPort;
        this.staleRecoveryService = staleRecoveryService;
        this.staleDetector = staleDetector;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RelatorioFolhaDTO gerarFolha(String login, int mes, int ano) {
        ProcessamentoHandle handle = transactionTemplate.execute(
            status -> iniciarGeracao(login, RelatorioTipo.FOLHA, mes, ano));
        aguardarProcessamento(handle.future());
        return toFolhaDto(recarregar(handle.relatorioId()));
    }

    public RelatorioBeneficioDTO gerarBeneficio(String login, int mes, int ano) {
        ProcessamentoHandle handle = transactionTemplate.execute(
            status -> iniciarGeracao(login, RelatorioTipo.BENEFICIO, mes, ano));
        aguardarProcessamento(handle.future());
        return toBeneficioDto(recarregar(handle.relatorioId()));
    }

    @Transactional
    public List<RelatorioFolhaDTO> listarFolha(String login) {
        Usuario usuario = obterUsuario(login);
        staleRecoveryService.recuperarParaUsuario(usuario.getId());
        return relatorioRepository
            .findByUsuarioIdAndTipoAndAtivoTrueOrderByAnoDescMesDesc(usuario.getId(), RelatorioTipo.FOLHA)
            .stream()
            .map(this::toFolhaDto)
            .toList();
    }

    @Transactional
    public List<RelatorioBeneficioDTO> listarBeneficio(String login) {
        Usuario usuario = obterUsuario(login);
        staleRecoveryService.recuperarParaUsuario(usuario.getId());
        return relatorioRepository
            .findByUsuarioIdAndTipoAndAtivoTrueOrderByAnoDescMesDesc(usuario.getId(), RelatorioTipo.BENEFICIO)
            .stream()
            .map(this::toBeneficioDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public byte[] downloadPdf(String login, Long id, RelatorioTipo tipo) {
        Usuario usuario = obterUsuario(login);
        Relatorio relatorio = relatorioRepository.findByIdAndUsuarioIdAndAtivoTrue(id, usuario.getId())
            .orElseThrow(() -> new RelatorioNotFoundException(id));

        if (relatorio.getTipo() != tipo) {
            throw new RelatorioNotFoundException(id);
        }
        if (relatorio.getStatus() != RelatorioStatus.PROCESSADO) {
            throw new RelatorioIndisponivelException(relatorio.getStatus());
        }

        return relatorioArquivoRepository.findByRelatorioId(id)
            .map(arquivo -> arquivo.getPdfBytes())
            .orElseThrow(() -> new RelatorioIndisponivelException(relatorio.getStatus()));
    }

    private ProcessamentoHandle iniciarGeracao(String login, RelatorioTipo tipo, int mes, int ano) {
        validarCompetencia(mes, ano);
        Usuario usuario = obterUsuario(login);
        validarAcesso(usuario.getId());

        staleRecoveryService.recuperarParaUsuario(usuario.getId());

        Relatorio relatorio = relatorioRepository
            .findByUsuarioIdAndTipoAndMesAndAnoAndAtivoTrue(usuario.getId(), tipo, mes, ano)
            .orElseGet(() -> criarNovoRelatorio(usuario, tipo, mes, ano));

        if (relatorio.getId() != null) {
            relatorio = recarregar(relatorio.getId());
        }

        boolean jaPendente = relatorio.getStatus() == RelatorioStatus.PENDENTE;
        if (!jaPendente) {
            long pendentes = staleRecoveryService.contarPendentesAtivos(usuario.getId());
            if (pendentes >= properties.getMaxJobsSimultaneosPorUsuario()) {
                throw new RelatorioGeracaoLimiteException(properties.getMaxJobsSimultaneosPorUsuario());
            }
        }

        relatorioArquivoRepository.findByRelatorioId(relatorio.getId())
            .ifPresent(relatorioArquivoRepository::delete);

        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setErro(null);
        relatorio.setDataProcessamento(null);
        relatorio.setTotalFuncionarios(null);
        relatorio.setTotalFolha(null);
        relatorio.setTotalBeneficios(null);
        relatorio.setTotalValor(null);
        relatorio = relatorioRepository.save(relatorio);
        CompletableFuture<Void> future = enfileirarProcessamentoAposCommit(relatorio.getId());
        return new ProcessamentoHandle(relatorio.getId(), future);
    }

    private CompletableFuture<Void> enfileirarProcessamentoAposCommit(Long relatorioId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    relatorioGeracaoWorker.processar(relatorioId)
                        .whenComplete((result, error) -> {
                            if (error != null) {
                                future.completeExceptionally(error);
                            } else {
                                future.complete(null);
                            }
                        });
                }
            });
        } else {
            relatorioGeracaoWorker.processar(relatorioId)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        future.completeExceptionally(error);
                    } else {
                        future.complete(null);
                    }
                });
        }
        return future;
    }

    void enfileirarProcessamento(Long relatorioId) {
        enfileirarProcessamentoAposCommit(relatorioId);
    }

    private record ProcessamentoHandle(Long relatorioId, CompletableFuture<Void> future) {}

    private Relatorio criarNovoRelatorio(Usuario usuario, RelatorioTipo tipo, int mes, int ano) {
        long pendentes = staleRecoveryService.contarPendentesAtivos(usuario.getId());
        if (pendentes >= properties.getMaxJobsSimultaneosPorUsuario()) {
            throw new RelatorioGeracaoLimiteException(properties.getMaxJobsSimultaneosPorUsuario());
        }
        Relatorio relatorio = new Relatorio();
        relatorio.setUsuario(usuario);
        relatorio.setTipo(tipo);
        relatorio.setMes(mes);
        relatorio.setAno(ano);
        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setAtivo(true);
        return relatorio;
    }

    private void aguardarProcessamento(CompletableFuture<Void> future) {
        try {
            future.get(properties.getTimeoutSegundos(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // REL-01: timeout → retorna PENDENTE (polling no FE)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Geração do relatório interrompida", e);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao aguardar geração do relatório", e);
        }
    }

    private Relatorio recarregar(Long id) {
        return relatorioRepository.findById(id).orElseThrow(() -> new RelatorioNotFoundException(id));
    }

    private void validarCompetencia(int mes, int ano) {
        YearMonth competencia = YearMonth.of(ano, mes);
        YearMonth atual = YearMonth.now(Clock.systemDefaultZone());
        if (competencia.isAfter(atual)) {
            throw new IllegalArgumentException("Competência futura não permitida");
        }
    }

    private void validarAcesso(Long usuarioId) {
        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioId);
        if (contexto.acessoTotal()) {
            return;
        }
        if (contexto.motivoNegacao() != null) {
            throw new RelatorioAcessoNegadoException();
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            throw new RelatorioAcessoNegadoException();
        }
        if (contexto.centrosCustoIds() == null || contexto.centrosCustoIds().isEmpty()) {
            throw new RelatorioAcessoNegadoException();
        }
    }

    private Usuario obterUsuario(String login) {
        return usuarioLookupPort.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    private boolean isStale(Relatorio relatorio) {
        if (relatorio.getStatus() != RelatorioStatus.PENDENTE) {
            return false;
        }
        boolean hasBlob = relatorio.getId() != null
            && relatorioArquivoRepository.findByRelatorioId(relatorio.getId()).isPresent();
        return staleDetector.isStale(relatorio, hasBlob);
    }

    private RelatorioFolhaDTO toFolhaDto(Relatorio relatorio) {
        return new RelatorioFolhaDTO(
            relatorio.getId(),
            relatorio.getMes(),
            relatorio.getAno(),
            relatorio.getTotalFuncionarios(),
            relatorio.getTotalFolha(),
            relatorio.getTotalBeneficios(),
            relatorio.getStatus(),
            relatorio.getDataProcessamento(),
            relatorio.getErro(),
            relatorio.getDataCriacao(),
            isStale(relatorio)
        );
    }

    private RelatorioBeneficioDTO toBeneficioDto(Relatorio relatorio) {
        return new RelatorioBeneficioDTO(
            relatorio.getId(),
            relatorio.getMes(),
            relatorio.getAno(),
            relatorio.getTotalBeneficios(),
            relatorio.getTotalValor(),
            relatorio.getStatus(),
            relatorio.getDataProcessamento(),
            relatorio.getErro(),
            relatorio.getDataCriacao(),
            isStale(relatorio)
        );
    }
}
