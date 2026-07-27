package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.AcessoUsuarioDTO;
import br.com.techne.sistemafolha.auth.api.LoginDTO;
import br.com.techne.sistemafolha.auth.api.TokenDTO;
import br.com.techne.sistemafolha.auth.domain.RefreshToken;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String DOMAIN = "auth";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    @Transactional
    public TokenDTO authenticate(LoginDTO loginDTO) {
        logger.info("{}Iniciando autenticação para o usuário: {}", DomainLogging.prefix(DOMAIN), loginDTO.login());
        
        try {
            Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(loginDTO.login())
                    .orElseThrow(() -> {
                        logger.error("Usuário não encontrado: {}", loginDTO.login());
                        return new UsernameNotFoundException("Usuário não encontrado");
                    });

            logger.debug("Hash da senha armazenada: {}", usuario.getSenha());
            
            if (!passwordEncoder.matches(loginDTO.senha(), usuario.getSenha())) {
                logger.error("Senha incorreta para o usuário: {}", loginDTO.login());
                throw new UsernameNotFoundException("Senha incorreta");
            }

            logger.info("Senha verificada com sucesso para o usuário: {}", loginDTO.login());

            UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.login());
            String jwtToken = jwtService.generateToken(userDetails);
            
            RefreshToken refreshToken = refreshTokenService.criarRefreshToken(loginDTO.login());
            
            LocalDateTime tokenExpiration = LocalDateTime.now().plusSeconds(jwtService.getJwtExpirationTime() / 1000);
            LocalDateTime refreshExpiration = refreshToken.getDataExpiracao();
            
            AcessoUsuarioDTO acessoUsuario = obterAcessoUsuario(usuario.getId());
            
            logger.info("Token JWT e refresh token gerados com sucesso para o usuário: {}", loginDTO.login());

            return new TokenDTO(
                loginDTO.login(), 
                jwtToken, 
                refreshToken.getToken(),
                tokenExpiration,
                refreshExpiration,
                acessoUsuario
            );
        } catch (Exception e) {
            logger.error("Falha na autenticação para o usuário {}: {}", loginDTO.login(), e.getMessage());
            throw new UsernameNotFoundException("Usuário ou senha inválidos");
        }
    }

    @Transactional
    public TokenDTO refreshToken(String refreshTokenString) {
        logger.info("Processando refresh token");
        
        RefreshToken refreshToken = refreshTokenService.buscarPorToken(refreshTokenString)
                .orElseThrow(() -> new RuntimeException("Refresh token não encontrado"));
        
        if (!refreshTokenService.validarRefreshToken(refreshToken)) {
            throw new RuntimeException("Refresh token inválido ou expirado");
        }
        
        Usuario usuario = refreshToken.getUsuario();
        logger.info("Gerando novo token para o usuário: {}", usuario.getLogin());
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getLogin());
        String newJwtToken = jwtService.generateToken(userDetails);
        
        RefreshToken newRefreshToken = refreshTokenService.criarRefreshToken(usuario.getLogin());
        
        LocalDateTime tokenExpiration = LocalDateTime.now().plusSeconds(jwtService.getJwtExpirationTime() / 1000);
        LocalDateTime refreshExpiration = newRefreshToken.getDataExpiracao();
        
        AcessoUsuarioDTO acessoUsuario = obterAcessoUsuario(usuario.getId());
        
        logger.info("Tokens renovados com sucesso para o usuário: {}", usuario.getLogin());
        
        return new TokenDTO(
            usuario.getLogin(), 
            newJwtToken, 
            newRefreshToken.getToken(),
            tokenExpiration,
            refreshExpiration,
            acessoUsuario
        );
    }

    @Transactional
    public void logout(String refreshTokenString) {
        logger.info("Processando logout");
        
        if (refreshTokenString != null && !refreshTokenString.isEmpty()) {
            refreshTokenService.revogarToken(refreshTokenString);
            logger.info("Refresh token revogado no logout");
        }
    }

    public AcessoUsuarioDTO obterAcessoUsuarioPorLogin(String login) {
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return obterAcessoUsuario(usuario.getId());
    }

    public AcessoUsuarioDTO obterAcessoUsuario(Long usuarioId) {
        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioId);
        return AcessoUsuarioDTO.builder()
            .temFuncionarioVinculado(contexto.temFuncionarioVinculado())
            .temNoOrganograma(contexto.temNoOrganograma())
            .acessoTotal(contexto.acessoTotal())
            .centrosCustoIds(contexto.centrosCustoIds())
            .motivoNegacao(contexto.motivoNegacao())
            .noOrganogramaId(contexto.noOrganogramaId())
            .noOrganogramaNome(contexto.noOrganogramaNome())
            .nivel(contexto.nivel())
            .quantidadeCentrosAcessiveis(contexto.centrosCustoIds().size())
            .build();
    }
}
