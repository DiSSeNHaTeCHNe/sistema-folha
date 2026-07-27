package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalDTO;
import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalResumoDTO;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficio;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalResumoProjection;
import br.com.techne.sistemafolha.beneficios.infrastructure.TipoBeneficioRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficioMensalService {

    private final BeneficioMensalRepository beneficioMensalRepository;
    private final FuncionarioConsultaPort funcionarioConsultaPort;
    private final TipoBeneficioRepository tipoBeneficioRepository;
    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    public List<BeneficioMensalDTO> listarPorCompetenciaParaUsuario(
            String login, LocalDate dataInicio, LocalDate dataFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return List.of();
        }
        if (!contexto.acessoTotal() && centrosVazios(contexto)) {
            return List.of();
        }
        return listarPorCompetencia(dataInicio, dataFim, centrosParaFiltro(contexto));
    }

    public List<BeneficioMensalResumoDTO> resumoPorCompetenciaParaUsuario(
            String login, LocalDate dataInicio, LocalDate dataFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return List.of();
        }
        if (!contexto.acessoTotal() && centrosVazios(contexto)) {
            return List.of();
        }
        return resumoPorCompetencia(dataInicio, dataFim, centrosParaFiltro(contexto));
    }

    public List<BeneficioMensalDTO> listarPorFuncionarioParaUsuario(
            String login, Long funcionarioId, LocalDate dataInicio, LocalDate dataFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return List.of();
        }
        return listarPorFuncionario(funcionarioId, dataInicio, dataFim)
            .stream()
            .filter(dto -> aplicarFiltroAcesso(dto, contexto))
            .collect(Collectors.toList());
    }

    public Optional<BeneficioMensalDTO> criarParaUsuario(String login, BeneficioMensalDTO dto) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        Funcionario funcionario = funcionarioConsultaPort.findByIdAndAtivoTrue(dto.funcionarioId())
            .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
        if (!aplicarFiltroAcesso(funcionario, contexto)) {
            return Optional.empty();
        }
        return Optional.of(criar(dto));
    }

    public boolean removerSeAutorizado(String login, Long id) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        return beneficioMensalRepository.findById(id)
            .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
            .filter(b -> aplicarFiltroAcesso(b, contexto))
            .map(b -> {
                remover(id);
                return true;
            })
            .orElse(false);
    }

    public List<BeneficioMensalDTO> listarPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        return buscarPorCompetencia(dataInicio, dataFim, centros).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<BeneficioMensalResumoDTO> resumoPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        return buscarResumoPorCompetencia(dataInicio, dataFim, centros).stream()
                .map(this::toResumoDTO)
                .collect(Collectors.toList());
    }

    public List<BeneficioMensalDTO> listarPorFuncionario(
            Long funcionarioId, LocalDate dataInicio, LocalDate dataFim) {
        return beneficioMensalRepository
                .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                        funcionarioId, dataInicio, dataFim)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public BeneficioMensalDTO criar(BeneficioMensalDTO dto) {
        Funcionario funcionario = funcionarioConsultaPort.findByIdAndAtivoTrue(dto.funcionarioId())
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));

        TipoBeneficio tipoBeneficio = tipoBeneficioRepository.findById(dto.tipoBeneficioId())
                .filter(t -> Boolean.TRUE.equals(t.getAtivo()))
                .orElseThrow(() -> new TipoBeneficioNotFoundException(dto.tipoBeneficioId()));

        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setFuncionario(funcionario);
        beneficio.setTipoBeneficio(tipoBeneficio);
        beneficio.setValor(dto.valor());
        beneficio.setCompetenciaInicio(dto.competenciaInicio());
        beneficio.setCompetenciaFim(dto.competenciaFim());
        beneficio.setObservacao(dto.observacao());
        beneficio.setAtivo(true);

        return toDTO(beneficioMensalRepository.save(beneficio));
    }

    @Transactional
    public void remover(Long id) {
        BeneficioMensal beneficio = beneficioMensalRepository.findById(id)
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .orElseThrow(() -> new BeneficioMensalNotFoundException(id));
        beneficio.setAtivo(false);
        beneficioMensalRepository.save(beneficio);
    }

    private AccessContextDTO obterContextoAcesso(String login) {
        Usuario usuario = usuarioLookupPort.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return organogramaAcessoPort.obterContextoAcesso(usuario.getId());
    }

    private boolean acessoNegado(AccessContextDTO contexto) {
        return !contexto.acessoTotal()
            && (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma());
    }

    private Set<Long> centrosParaFiltro(AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return Collections.emptySet();
        }
        return contexto.centrosCustoIds();
    }

    private boolean centrosVazios(AccessContextDTO contexto) {
        Set<Long> centros = contexto.centrosCustoIds();
        return centros == null || centros.isEmpty();
    }

    private boolean aplicarFiltroAcesso(BeneficioMensal beneficio, AccessContextDTO contexto) {
        return aplicarFiltroAcesso(beneficio.getFuncionario(), contexto);
    }

    boolean aplicarFiltroAcesso(Funcionario funcionario, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        if (funcionario != null && funcionario.getCentroCusto() != null) {
            return contexto.centrosCustoIds().contains(funcionario.getCentroCusto().getId());
        }
        return false;
    }

    private boolean aplicarFiltroAcesso(BeneficioMensalDTO dto, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        if (dto.centroCustoId() != null) {
            return contexto.centrosCustoIds().contains(dto.centroCustoId());
        }
        return false;
    }

    private List<BeneficioMensal> buscarPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        if (centros.isEmpty()) {
            return beneficioMensalRepository
                    .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(dataInicio, dataFim);
        }
        return beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue(
                        dataInicio, dataFim, centros);
    }

    private List<BeneficioMensalResumoProjection> buscarResumoPorCompetencia(
            LocalDate dataInicio, LocalDate dataFim, Set<Long> centros) {
        if (centros.isEmpty()) {
            return beneficioMensalRepository.resumoPorCompetencia(dataInicio, dataFim);
        }
        return beneficioMensalRepository.resumoPorCompetenciaAndCentroCustoIds(
                dataInicio, dataFim, centros);
    }

    private BeneficioMensalDTO toDTO(BeneficioMensal beneficio) {
        Funcionario funcionario = beneficio.getFuncionario();
        TipoBeneficio tipo = beneficio.getTipoBeneficio();

        Long centroCustoId = null;
        String centroCustoDescricao = null;
        if (funcionario != null && funcionario.getCentroCusto() != null) {
            centroCustoId = funcionario.getCentroCusto().getId();
            centroCustoDescricao = funcionario.getCentroCusto().getDescricao();
        }

        return new BeneficioMensalDTO(
                beneficio.getId(),
                funcionario != null ? funcionario.getId() : null,
                funcionario != null ? funcionario.getNome() : null,
                tipo != null ? tipo.getId() : null,
                tipo != null ? tipo.getCodigo() : null,
                tipo != null ? tipo.getDescricao() : null,
                centroCustoId,
                centroCustoDescricao,
                beneficio.getValor(),
                beneficio.getCompetenciaInicio(),
                beneficio.getCompetenciaFim(),
                beneficio.getObservacao()
        );
    }

    private BeneficioMensalResumoDTO toResumoDTO(BeneficioMensalResumoProjection projection) {
        return new BeneficioMensalResumoDTO(
                projection.getCodigo(),
                projection.getDescricao(),
                projection.getTotal(),
                projection.getQtdLancamentos()
        );
    }
}
