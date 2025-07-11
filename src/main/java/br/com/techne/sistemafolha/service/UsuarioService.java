package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.UsuarioDTO;
import br.com.techne.sistemafolha.exception.UsuarioNotFoundException;
import br.com.techne.sistemafolha.exception.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.model.Usuario;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.repository.UsuarioRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioDTO> listarTodos() {
        logger.info("Listando todos os usuários");
        return usuarioRepository.findAll().stream()
                .filter(u -> u.isAtivo())
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public UsuarioDTO buscarPorId(Long id) {
        logger.info("Buscando usuário por ID: {}", id);
        return usuarioRepository.findById(id)
                .filter(u -> u.isAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    public UsuarioDTO buscarPorLogin(String login) {
        logger.info("Buscando usuário por login: {}", login);
        return usuarioRepository.findByLoginAndAtivoTrue(login)
                .filter(u -> u.isAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new UsuarioNotFoundException(login));
    }

    public UsuarioDTO buscarPorFuncionario(Long funcionarioId) {
        logger.info("Buscando usuário por funcionário ID: {}", funcionarioId);
        return usuarioRepository.findByFuncionarioIdAndAtivoTrue(funcionarioId)
                .map(this::toDTO)
                .orElse(null);
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
                .filter(u -> u.isAtivo())
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
            Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
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
                .filter(u -> u.isAtivo())
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
            Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
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

        logger.debug("Hash da senha atual armazenada: {}", usuario.getSenha());
        logger.debug("Hash da senha atual fornecida: {}", passwordEncoder.encode(senhaAtual));

        if (!verificarSenha(senhaAtual, usuario.getSenha())) {
            logger.error("Senha atual incorreta para o usuário: {}", usuario.getLogin());
            throw new RuntimeException("Senha atual incorreta");
        }

        logger.info("Senha atual verificada com sucesso para o usuário: {}", usuario.getLogin());
        String novaSenhaCriptografada = passwordEncoder.encode(novaSenha);
        usuario.setSenha(novaSenhaCriptografada);
        logger.debug("Nova senha criptografada: {}", novaSenhaCriptografada);
        usuarioRepository.save(usuario);
        logger.info("Senha alterada com sucesso para o usuário: {}", usuario.getLogin());
    }

    public boolean verificarSenha(String senhaTexto, String senhaHash) {
        logger.debug("Verificando senha");
        logger.debug("Senha em texto: {}", senhaTexto);
        logger.debug("Hash armazenado: {}", senhaHash);
        logger.debug("Hash gerado para comparação: {}", passwordEncoder.encode(senhaTexto));
        boolean resultado = passwordEncoder.matches(senhaTexto, senhaHash);
        logger.debug("Resultado da verificação: {}", resultado);
        return resultado;
    }
} 