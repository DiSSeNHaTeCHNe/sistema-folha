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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class RelatorioGeracaoService {

    private final RelatorioRepository relatorioRepository;
    private final RelatorioArquivoRepository relatorioArquivoRepository;
    private final RelatorioGeracaoWorker relatorioGeracaoWorker;
    private final RelatorioGeracaoProperties properties;
    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    @Transactional
    public RelatorioFolhaDTO gerarFolha(String login, int mes, int ano) {
        Relatorio relatorio = iniciarGeracao(login, RelatorioTipo.FOLHA, mes, ano);
        aguardarProcessamento(relatorio.getId());
        return toFolhaDto(recarregar(relatorio.getId()));
    }

    @Transactional
    public RelatorioBeneficioDTO gerarBeneficio(String login, int mes, int ano) {
        Relatorio relatorio = iniciarGeracao(login, RelatorioTipo.BENEFICIO, mes, ano);
        aguardarProcessamento(relatorio.getId());
        return toBeneficioDto(recarregar(relatorio.getId()));
    }

    @Transactional(readOnly = true)
    public List<RelatorioFolhaDTO> listarFolha(String login) {
        Usuario usuario = obterUsuario(login);
        return relatorioRepository
            .findByUsuarioIdAndTipoAndAtivoTrueOrderByAnoDescMesDesc(usuario.getId(), RelatorioTipo.FOLHA)
            .stream()
            .map(this::toFolhaDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<RelatorioBeneficioDTO> listarBeneficio(String login) {
        Usuario usuario = obterUsuario(login);
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

    private Relatorio iniciarGeracao(String login, RelatorioTipo tipo, int mes, int ano) {
        validarCompetencia(mes, ano);
        Usuario usuario = obterUsuario(login);
        validarAcesso(usuario.getId());

        Relatorio relatorio = relatorioRepository
            .findByUsuarioIdAndTipoAndMesAndAnoAndAtivoTrue(usuario.getId(), tipo, mes, ano)
            .orElseGet(() -> criarNovoRelatorio(usuario, tipo, mes, ano));

        boolean jaPendente = relatorio.getStatus() == RelatorioStatus.PENDENTE;
        if (!jaPendente) {
            long pendentes = relatorioRepository.countByUsuarioIdAndStatusAndAtivoTrue(
                usuario.getId(), RelatorioStatus.PENDENTE);
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
        return relatorio;
    }

    private Relatorio criarNovoRelatorio(Usuario usuario, RelatorioTipo tipo, int mes, int ano) {
        long pendentes = relatorioRepository.countByUsuarioIdAndStatusAndAtivoTrue(
            usuario.getId(), RelatorioStatus.PENDENTE);
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

    private void aguardarProcessamento(Long relatorioId) {
        try {
            CompletableFuture<Void> future = relatorioGeracaoWorker.processar(relatorioId);
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
            relatorio.getErro()
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
            relatorio.getErro()
        );
    }
}
