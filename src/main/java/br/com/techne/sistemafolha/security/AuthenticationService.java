package br.com.techne.sistemafolha.security;

import br.com.techne.sistemafolha.dto.AcessoUsuarioDTO;
import br.com.techne.sistemafolha.dto.LoginDTO;
import br.com.techne.sistemafolha.dto.TokenDTO;
import br.com.techne.sistemafolha.model.NoOrganograma;
import br.com.techne.sistemafolha.model.RefreshToken;
import br.com.techne.sistemafolha.model.Usuario;
import br.com.techne.sistemafolha.repository.UsuarioRepository;
import br.com.techne.sistemafolha.service.OrganogramaAcessoService;
import br.com.techne.sistemafolha.service.RefreshTokenService;
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
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final OrganogramaAcessoService organogramaAcessoService;

    @Transactional
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
            String jwtToken = jwtService.generateToken(userDetails);
            
            // Gera o refresh token
            RefreshToken refreshToken = refreshTokenService.criarRefreshToken(loginDTO.login());
            
            // Calcula as datas de expiração
            LocalDateTime tokenExpiration = LocalDateTime.now().plusSeconds(jwtService.getJwtExpirationTime() / 1000);
            LocalDateTime refreshExpiration = refreshToken.getDataExpiracao();
            
            // Busca informações de acesso baseadas no organograma
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
        
        // Gera novo access token
        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getLogin());
        String newJwtToken = jwtService.generateToken(userDetails);
        
        // Cria novo refresh token
        RefreshToken newRefreshToken = refreshTokenService.criarRefreshToken(usuario.getLogin());
        
        // Calcula as datas de expiração
        LocalDateTime tokenExpiration = LocalDateTime.now().plusSeconds(jwtService.getJwtExpirationTime() / 1000);
        LocalDateTime refreshExpiration = newRefreshToken.getDataExpiracao();
        
        // Busca informações de acesso baseadas no organograma
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
    
    /**
     * Obtém as informações de acesso do usuário baseadas no organograma.
     */
    private AcessoUsuarioDTO obterAcessoUsuario(Long usuarioId) {
        Optional<NoOrganograma> noOpt = organogramaAcessoService.obterNoDoUsuario(usuarioId);
        Set<Long> centrosAcessiveis = organogramaAcessoService.obterCentrosCustoAcessiveis(usuarioId);
        boolean acessoTotal = organogramaAcessoService.usuarioTemAcessoTotal(usuarioId);
        
        AcessoUsuarioDTO.AcessoUsuarioDTOBuilder builder = AcessoUsuarioDTO.builder()
            .centrosCustoAcessiveis(centrosAcessiveis)
            .acessoTotal(acessoTotal)
            .quantidadeCentrosAcessiveis(centrosAcessiveis.size());
        
        if (noOpt.isPresent()) {
            NoOrganograma no = noOpt.get();
            builder.noOrganogramaId(no.getId())
                   .noOrganogramaNome(no.getNome())
                   .nivel(no.getNivel());
        }
        
        return builder.build();
    }
} 