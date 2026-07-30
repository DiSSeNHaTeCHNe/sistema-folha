package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.UsuarioDTO;
import br.com.techne.sistemafolha.auth.domain.UsuarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.shared.access.CentroCustoEfetivo;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);
    private static final String DOMAIN = "auth";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioConsultaPort funcionarioConsultaPort;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    public List<UsuarioDTO> listarTodos() {
        return listar(null, null, null);
    }

    public List<UsuarioDTO> listar(String nome, String login, Long funcionarioId) {
        logger.info("{}Listando usuários com filtros", DOMAIN_PREFIX);

        String nomePattern = null;
        if (nome != null && !nome.trim().isEmpty()) {
            nomePattern = "%" + nome.trim() + "%";
        }

        String loginPattern = null;
        if (login != null && !login.trim().isEmpty()) {
            loginPattern = "%" + login.trim() + "%";
        }

        return usuarioRepository.findByFiltros(nomePattern, loginPattern, funcionarioId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public UsuarioDTO buscarPorId(Long id) {
        logger.info("Buscando usuário por ID: {}", id);
        return usuarioRepository.findById(id)
                .filter(Usuario::isAtivo)
                .map(this::toDTO)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    public UsuarioDTO buscarPorLogin(String login) {
        logger.info("Buscando usuário por login: {}", login);
        return usuarioRepository.findByLoginAndAtivoTrue(login)
                .filter(Usuario::isAtivo)
                .map(this::toDTO)
                .orElseThrow(() -> new UsuarioNotFoundException(login));
    }

    public UsuarioDTO buscarPorFuncionario(Long funcionarioId) {
        logger.info("Buscando usuário por funcionário ID: {}", funcionarioId);
        return usuarioRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId)
                .map(this::toDTO)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarParaUsuario(String login, String nome, String loginFilter, Long funcionarioId) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return List.of();
        }
        if (contexto.acessoTotal()) {
            return listar(nome, loginFilter, funcionarioId);
        }
        if (centrosVazios(contexto)) {
            return List.of();
        }
        String nomePattern = null;
        if (nome != null && !nome.trim().isEmpty()) {
            nomePattern = "%" + nome.trim() + "%";
        }
        String loginPattern = null;
        if (loginFilter != null && !loginFilter.trim().isEmpty()) {
            loginPattern = "%" + loginFilter.trim() + "%";
        }
        return usuarioRepository.findByFiltros(nomePattern, loginPattern, funcionarioId)
                .stream()
                .filter(u -> usuarioNoEscopo(u, contexto))
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorIdParaUsuario(String login, Long id) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            throw new UsuarioNotFoundException(id);
        }
        if (contexto.acessoTotal()) {
            return buscarPorId(id);
        }
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
        if (!usuarioNoEscopo(usuario, contexto)) {
            throw new UsuarioNotFoundException(id);
        }
        return toDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorLoginParaUsuario(String login, String alvoLogin) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            throw new UsuarioNotFoundException(alvoLogin);
        }
        if (contexto.acessoTotal()) {
            return buscarPorLogin(alvoLogin);
        }
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(alvoLogin)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsuarioNotFoundException(alvoLogin));
        if (!usuarioNoEscopo(usuario, contexto)) {
            throw new UsuarioNotFoundException(alvoLogin);
        }
        return toDTO(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioDTO buscarPorFuncionarioParaUsuario(String login, Long funcionarioId) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return null;
        }
        if (!contexto.acessoTotal()) {
            Funcionario funcionario = funcionarioConsultaPort.findById(funcionarioId).orElse(null);
            if (funcionario == null || !funcionarioNoEscopo(funcionario, contexto)) {
                return null;
            }
        }
        return buscarPorFuncionario(funcionarioId);
    }

    @Transactional
    public UsuarioDTO cadastrar(UsuarioDTO dto) {
        logger.info("Cadastrando novo usuário: {}", dto.login());
        if (usuarioRepository.existsByLoginAndAtivoTrue(dto.login())) {
            throw new IllegalArgumentException("Já existe um usuário ativo com este login");
        }

        Usuario usuario = toEntity(dto);
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        return toDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO atualizar(Long id, UsuarioDTO dto) {
        logger.info("Atualizando usuário ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        if (!usuario.getLogin().equals(dto.login()) && 
            usuarioRepository.existsByLoginAndAtivoTrue(dto.login())) {
            throw new IllegalArgumentException("Já existe um usuário ativo com este login");
        }

        usuario.setLogin(dto.login());
        usuario.setNome(dto.nome());
        usuario.setPermissoes(dto.permissoes());
        
        if (dto.senha() != null && !dto.senha().isEmpty()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }
        
        // Atualizar funcionário associado
        if (dto.funcionarioId() != null) {
            Funcionario funcionario = funcionarioConsultaPort.findById(dto.funcionarioId())
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
            usuario.setFuncionario(funcionario);
        } else {
            usuario.setFuncionario(null);
        }
        
        return toDTO(usuarioRepository.save(usuario));
    }

    @Transactional
    public void remover(Long id) {
        logger.info("Removendo usuário ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return UsuarioDTO.fromEntity(usuario);
    }

    private Usuario toEntity(UsuarioDTO dto) {
        Usuario usuario = new Usuario();
        usuario.setLogin(dto.login());
        usuario.setSenha(dto.senha());
        usuario.setNome(dto.nome());
        usuario.setPermissoes(dto.permissoes());
        usuario.setAtivo(true);
        
        if (dto.funcionarioId() != null) {
            Funcionario funcionario = funcionarioConsultaPort.findById(dto.funcionarioId())
                .orElseThrow(() -> new FuncionarioNotFoundException(dto.funcionarioId()));
            usuario.setFuncionario(funcionario);
        }
        
        return usuario;
    }

    @Transactional
    public void alterarSenha(Long id, String senhaAtual, String novaSenha) {
        logger.info("Alterando senha para o usuário ID: {}", id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!verificarSenha(senhaAtual, usuario.getSenha())) {
            logger.error("Senha atual incorreta para o usuário: {}", usuario.getLogin());
            throw new RuntimeException("Senha atual incorreta");
        }

        logger.info("Senha atual verificada com sucesso para o usuário: {}", usuario.getLogin());
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
        logger.info("Senha alterada com sucesso para o usuário: {}", usuario.getLogin());
    }

    public boolean verificarSenha(String senhaTexto, String senhaHash) {
        return passwordEncoder.matches(senhaTexto, senhaHash);
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

    private boolean usuarioNoEscopo(Usuario usuario, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        return funcionarioNoEscopo(usuario.getFuncionario(), contexto);
    }

    private boolean funcionarioNoEscopo(Funcionario funcionario, AccessContextDTO contexto) {
        if (funcionario == null) {
            return false;
        }
        CentroCusto centroCusto = funcionario.getCentroCusto();
        if (centroCusto == null) {
            return false;
        }
        return CentroCustoEfetivo.pertenceAoEscopo(centroCusto.getId(), contexto.centrosCustoIds());
    }
} 