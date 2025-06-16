package br.com.techne.sistemafolha.security;

import br.com.techne.sistemafolha.dto.LoginDTO;
import br.com.techne.sistemafolha.dto.TokenDTO;
import br.com.techne.sistemafolha.model.Usuario;
import br.com.techne.sistemafolha.repository.UsuarioRepository;
import br.com.techne.sistemafolha.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public TokenDTO authenticate(LoginDTO loginDTO) {
        logger.info("Iniciando autenticação para o usuário: {}", loginDTO.login());
        
        try {
            // Busca o usuário para verificação
            Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(loginDTO.login())
                    .orElseThrow(() -> {
                        logger.error("Usuário não encontrado: {}", loginDTO.login());
                        return new UsernameNotFoundException("Usuário não encontrado");
                    });

            logger.debug("Hash da senha armazenada: {}", usuario.getSenha());
            
            // Verifica a senha usando o método matches do BCrypt
            if (!passwordEncoder.matches(loginDTO.senha(), usuario.getSenha())) {
                logger.error("Senha incorreta para o usuário: {}", loginDTO.login());
                throw new UsernameNotFoundException("Senha incorreta");
            }

            logger.info("Senha verificada com sucesso para o usuário: {}", loginDTO.login());

            // Gera o token JWT
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.login());
            var jwtToken = jwtService.generateToken(userDetails);
            logger.info("Token JWT gerado com sucesso para o usuário: {}", loginDTO.login());

            return new TokenDTO(loginDTO.login(), jwtToken);
        } catch (Exception e) {
            logger.error("Falha na autenticação para o usuário {}: {}", loginDTO.login(), e.getMessage());
            throw new UsernameNotFoundException("Usuário ou senha inválidos");
        }
    }
} 