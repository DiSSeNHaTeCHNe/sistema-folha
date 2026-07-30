package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.FuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.api.FuncionarioStatusFiltro;
import br.com.techne.sistemafolha.cadastros.domain.CargoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.cadastros.infrastructure.CargoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.shared.access.CentroCustoEfetivo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;
    private final CargoRepository cargoRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    public List<FuncionarioDTO> listar(String nome, Long cargoId, Long centroCustoId, Long linhaNegocioId,
                                       FuncionarioStatusFiltro status) {
        String nomePattern = null;
        if (nome != null && !nome.trim().isEmpty()) {
            nomePattern = "%" + nome + "%";
        }

        return funcionarioRepository
                .findByFiltros(nomePattern, cargoId, centroCustoId, linhaNegocioId, resolverAtivo(status))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("java:S2447") // null means no ativo filter (all statuses)
    private Boolean resolverAtivo(FuncionarioStatusFiltro status) {
        if (status == null || status == FuncionarioStatusFiltro.ATIVO) {
            return true;
        }
        if (status == FuncionarioStatusFiltro.INATIVO) {
            return false;
        }
        return null;
    }

    public FuncionarioDTO buscarPorId(Long id) {
        return funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .map(this::toDTO)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<FuncionarioDTO> listarParaUsuario(
            String login, String nome, Long cargoId, Long centroCustoId, Long linhaNegocioId,
            FuncionarioStatusFiltro status) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return List.of();
        }
        if (contexto.acessoTotal()) {
            return listar(nome, cargoId, centroCustoId, linhaNegocioId, status);
        }
        if (centrosVazios(contexto)) {
            return List.of();
        }
        if (centroCustoId != null && !contexto.centrosCustoIds().contains(centroCustoId)) {
            return List.of();
        }
        String nomePattern = null;
        if (nome != null && !nome.trim().isEmpty()) {
            nomePattern = "%" + nome + "%";
        }
        return funcionarioRepository
                .findByFiltros(nomePattern, cargoId, centroCustoId, linhaNegocioId, resolverAtivo(status))
                .stream()
                .filter(f -> aplicarFiltroAcesso(f, contexto))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FuncionarioDTO buscarPorIdParaUsuario(String login, Long id) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            throw new FuncionarioNotFoundException(id);
        }
        if (contexto.acessoTotal()) {
            return buscarPorId(id);
        }
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
        if (!aplicarFiltroAcesso(funcionario, contexto)) {
            throw new FuncionarioNotFoundException(id);
        }
        return toDTO(funcionario);
    }

    @Transactional
    public FuncionarioDTO cadastrar(FuncionarioDTO dto) {
        validarCpfAtivoUnico(dto.cpf(), null);
        validarIdExternoUnico(dto.idExterno(), null);

        Cargo cargo = cargoRepository.findById(dto.cargoId())
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(dto.cargoId()));

        CentroCusto centroCusto = centroCustoRepository.findById(dto.centroCustoId())
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(dto.centroCustoId()));

        Funcionario funcionario = toEntity(dto);
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        return toDTO(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public FuncionarioDTO atualizar(Long id, FuncionarioDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));

        if (!funcionario.getCpf().equals(dto.cpf())) {
            validarCpfAtivoUnico(dto.cpf(), id);
        }
        validarIdExternoUnico(dto.idExterno(), id);

        Cargo cargo = cargoRepository.findById(dto.cargoId())
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(dto.cargoId()));

        CentroCusto centroCusto = centroCustoRepository.findById(dto.centroCustoId())
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(dto.centroCustoId()));

        funcionario.setNome(dto.nome());
        funcionario.setCpf(dto.cpf());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setIdExterno(normalizarIdExterno(dto.idExterno()));
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        return toDTO(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public void remover(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }

    private void validarCpfAtivoUnico(String cpf, Long idExcluir) {
        boolean conflito = idExcluir == null
                ? funcionarioRepository.existsByCpfAndAtivoTrue(cpf)
                : funcionarioRepository.findByCpfAndAtivoTrue(cpf)
                        .filter(f -> !f.getId().equals(idExcluir))
                        .isPresent();
        if (conflito) {
            throw new IllegalArgumentException("Já existe um funcionário ativo com este CPF");
        }
    }

    private void validarIdExternoUnico(String idExterno, Long idExcluir) {
        String idNormalizado = normalizarIdExterno(idExterno);
        if (idNormalizado == null) {
            return;
        }
        boolean conflito = idExcluir == null
                ? funcionarioRepository.existsByIdExterno(idNormalizado)
                : funcionarioRepository.existsByIdExternoAndIdNot(idNormalizado, idExcluir);
        if (conflito) {
            throw new IllegalArgumentException("Já existe um funcionário com este ID externo (matrícula)");
        }
    }

    private String normalizarIdExterno(String idExterno) {
        if (idExterno == null) {
            return null;
        }
        String trimmed = idExterno.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private FuncionarioDTO toDTO(Funcionario funcionario) {
        return new FuncionarioDTO(
            funcionario.getId(),
            funcionario.getNome(),
            funcionario.getCpf(),
            funcionario.getDataAdmissao(),
            funcionario.getCargo().getId(),
            funcionario.getCargo().getDescricao(),
            funcionario.getCentroCusto().getId(),
            funcionario.getCentroCusto().getDescricao(),
            funcionario.getCentroCusto().getLinhaNegocio().getId(),
            funcionario.getCentroCusto().getLinhaNegocio().getDescricao(),
            funcionario.getIdExterno(),
            funcionario.getAtivo()
        );
    }

    private Funcionario toEntity(FuncionarioDTO dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.nome());
        funcionario.setCpf(dto.cpf());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setIdExterno(normalizarIdExterno(dto.idExterno()));
        funcionario.setAtivo(true);
        return funcionario;
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

    private boolean centrosVazios(AccessContextDTO contexto) {
        Set<Long> centros = contexto.centrosCustoIds();
        return centros == null || centros.isEmpty();
    }

    private boolean aplicarFiltroAcesso(Funcionario funcionario, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        if (funcionario == null || funcionario.getCentroCusto() == null) {
            return false;
        }
        return CentroCustoEfetivo.pertenceAoEscopo(
            funcionario.getCentroCusto().getId(), contexto.centrosCustoIds());
    }
}
