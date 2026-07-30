package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.LoginDTO;
import br.com.techne.sistemafolha.auth.api.TokenDTO;
import br.com.techne.sistemafolha.auth.domain.RefreshToken;
import br.com.techne.sistemafolha.auth.domain.RefreshTokenInvalidoException;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String LOGIN = "usuario.teste";
    private static final String SENHA = "senha123";
    private static final String MENSAGEM_GENERICA = "Usuário ou senha inválidos";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_loginInexistente_lancaMensagemGenerica() {
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(SENHA, AuthenticationService.DUMMY_BCRYPT_HASH)).thenReturn(false);

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> authenticationService.authenticate(new LoginDTO(LOGIN, SENHA)));

        assertEquals(MENSAGEM_GENERICA, ex.getMessage());
        verify(passwordEncoder).matches(SENHA, AuthenticationService.DUMMY_BCRYPT_HASH);
    }

    @Test
    void authenticate_senhaIncorreta_lancaMesmaMensagemGenerica() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA, usuario.getSenha())).thenReturn(false);

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> authenticationService.authenticate(new LoginDTO(LOGIN, SENHA)));

        assertEquals(MENSAGEM_GENERICA, ex.getMessage());
    }

    @Test
    void authenticate_falhaPosCredencial_lancaMensagemGenerica() {
        Usuario usuario = usuarioAtivo();
        UserDetails userDetails = User.withUsername(LOGIN).password("hash").authorities(List.of()).build();

        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA, usuario.getSenha())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(LOGIN)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenThrow(new RuntimeException("JWT indisponível"));

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> authenticationService.authenticate(new LoginDTO(LOGIN, SENHA)));

        assertEquals(MENSAGEM_GENERICA, ex.getMessage());
    }

    @Test
    void authenticate_credenciaisValidas_retornaTokenDTO() {
        Usuario usuario = usuarioAtivo();
        UserDetails userDetails = User.withUsername(LOGIN).password("hash").authorities(List.of()).build();
        RefreshToken refreshToken = refreshTokenValido();

        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA, usuario.getSenha())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(LOGIN)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.getJwtExpirationTime()).thenReturn(3_600_000L);
        when(refreshTokenService.criarRefreshToken(LOGIN)).thenReturn(refreshToken);
        when(organogramaAcessoPort.obterContextoAcesso(usuario.getId())).thenReturn(contextoAcessoVazio());

        TokenDTO result = authenticationService.authenticate(new LoginDTO(LOGIN, SENHA));

        assertEquals(LOGIN, result.login());
        assertEquals("jwt-token", result.token());
        assertEquals(refreshToken.getToken(), result.refreshToken());
        assertNotNull(result.acessoUsuario());
    }

    @Test
    void refreshToken_valido_retornaNovoTokenDTO() {
        RefreshToken refreshToken = refreshTokenValido();
        RefreshToken novoRefreshToken = refreshTokenValido();
        novoRefreshToken.setToken("novo-refresh");
        UserDetails userDetails = User.withUsername(LOGIN).password("hash").authorities(List.of()).build();

        when(refreshTokenService.buscarPorToken("refresh-antigo")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.validarRefreshToken(refreshToken)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(LOGIN)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-novo");
        when(jwtService.getJwtExpirationTime()).thenReturn(3_600_000L);
        when(refreshTokenService.criarRefreshToken(LOGIN)).thenReturn(novoRefreshToken);
        when(organogramaAcessoPort.obterContextoAcesso(1L)).thenReturn(contextoAcessoVazio());

        TokenDTO result = authenticationService.refreshToken("refresh-antigo");

        assertEquals(LOGIN, result.login());
        assertEquals("jwt-novo", result.token());
        assertEquals("novo-refresh", result.refreshToken());
    }

    @Test
    void refreshToken_inexistente_lancaMensagemInvalida() {
        when(refreshTokenService.buscarPorToken("invalido")).thenReturn(Optional.empty());

        RefreshTokenInvalidoException ex = assertThrows(RefreshTokenInvalidoException.class,
            () -> authenticationService.refreshToken("invalido"));

        assertEquals("Refresh token inválido ou expirado", ex.getMessage());
    }

    @Test
    void refreshToken_expiradoOuRevogado_lancaMensagemInvalida() {
        RefreshToken refreshToken = refreshTokenValido();
        when(refreshTokenService.buscarPorToken("refresh-expirado")).thenReturn(Optional.of(refreshToken));
        when(refreshTokenService.validarRefreshToken(refreshToken)).thenReturn(false);

        RefreshTokenInvalidoException ex = assertThrows(RefreshTokenInvalidoException.class,
            () -> authenticationService.refreshToken("refresh-expirado"));

        assertEquals("Refresh token inválido ou expirado", ex.getMessage());
    }

    @Test
    void logout_comRefreshToken_revogaToken() {
        authenticationService.logout("refresh-token");

        verify(refreshTokenService).revogarToken("refresh-token");
    }

    @Test
    void logout_semRefreshToken_naoRevoga() {
        authenticationService.logout(null);
        authenticationService.logout("");

        verify(refreshTokenService, never()).revogarToken(any());
    }

    private AccessContextDTO contextoAcessoVazio() {
        return new AccessContextDTO(false, false, false, Collections.emptySet(), null, null, null, null);
    }

    private RefreshToken refreshTokenValido() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-antigo");
        refreshToken.setUsuario(usuarioAtivo());
        refreshToken.setDataExpiracao(LocalDateTime.now().plusDays(1));
        refreshToken.setRevogado(false);
        return refreshToken;
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin(LOGIN);
        usuario.setSenha("hash-armazenado");
        usuario.setAtivo(true);
        return usuario;
    }
}
