package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.domain.RefreshToken;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.RefreshTokenRepository;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String DOMAIN = "auth";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final Clock clock;

    @Transactional
    public RefreshToken criarRefreshToken(String login) {
        logger.info("{}Criando refresh token para o usuário: {}", DOMAIN_PREFIX, login);
        
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + login));
        
        // Revogar tokens antigos do usuário
        refreshTokenRepository.revogarTodosPorUsuario(usuario);
        
        String token = jwtService.generateRefreshToken();
        LocalDateTime dataExpiracao = LocalDateTime.now(clock)
                .plusSeconds(jwtService.getRefreshExpirationTime() / 1000);
        
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUsuario(usuario);
        refreshToken.setDataExpiracao(dataExpiracao);
        refreshToken.setRevogado(false);
        
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> buscarPorToken(String token) {
        logger.debug("Buscando refresh token");
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revogarToken(String token) {
        logger.info("Revogando refresh token");
        refreshTokenRepository.revogarPorToken(token);
    }

    @Transactional
    public void revogarTodosPorUsuario(Usuario usuario) {
        logger.info("Revogando todos os refresh tokens do usuário: {}", usuario.getLogin());
        refreshTokenRepository.revogarTodosPorUsuario(usuario);
    }

    @Transactional
    public void limparTokensExpirados() {
        logger.info("Limpando refresh tokens expirados");
        refreshTokenRepository.deleteByDataExpiracaoBefore(LocalDateTime.now(clock));
    }

    public boolean validarRefreshToken(RefreshToken refreshToken) {
        if (refreshToken == null) {
            logger.warn("Refresh token é nulo");
            return false;
        }
        
        if (refreshToken.getRevogado()) {
            logger.warn("Refresh token inválido");
            return false;
        }

        if (LocalDateTime.now(clock).isAfter(refreshToken.getDataExpiracao())) {
            logger.warn("Refresh token inválido");
            return false;
        }
        
        logger.debug("Refresh token válido");
        return true;
    }
} 