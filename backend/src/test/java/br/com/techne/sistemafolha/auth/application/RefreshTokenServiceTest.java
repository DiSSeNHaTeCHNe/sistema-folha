package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.domain.RefreshToken;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.RefreshTokenRepository;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String LOGIN = "usuario.teste";
    private static final String TOKEN = "refresh-token-abc";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void criarRefreshToken_revogaAntigosEPersisteNovo() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(jwtService.generateRefreshToken()).thenReturn(TOKEN);
        when(jwtService.getRefreshExpirationTime()).thenReturn(86_400_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.criarRefreshToken(LOGIN);

        verify(refreshTokenRepository).revogarTodosPorUsuario(usuario);
        assertEquals(TOKEN, result.getToken());
        assertEquals(usuario, result.getUsuario());
        assertFalse(result.getRevogado());
    }

    @Test
    void criarRefreshToken_usuarioInexistente_lancaUsernameNotFoundException() {
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> refreshTokenService.criarRefreshToken(LOGIN));
    }

    @Test
    void buscarPorToken_delegatesToRepository() {
        RefreshToken refreshToken = refreshTokenValido();
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.buscarPorToken(TOKEN);

        assertTrue(result.isPresent());
        assertEquals(TOKEN, result.get().getToken());
    }

    @Test
    void revogarToken_delegatesToRepository() {
        refreshTokenService.revogarToken(TOKEN);

        verify(refreshTokenRepository).revogarPorToken(TOKEN);
    }

    @Test
    void revogarTodosPorUsuario_delegatesToRepository() {
        Usuario usuario = usuarioAtivo();

        refreshTokenService.revogarTodosPorUsuario(usuario);

        verify(refreshTokenRepository).revogarTodosPorUsuario(usuario);
    }

    @Test
    void limparTokensExpirados_delegatesToRepository() {
        refreshTokenService.limparTokensExpirados();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refreshTokenRepository).deleteByDataExpiracaoBefore(captor.capture());
    }

    @Test
    void validarRefreshToken_null_retornaFalse() {
        assertFalse(refreshTokenService.validarRefreshToken(null));
    }

    @Test
    void validarRefreshToken_expirado_retornaFalse() {
        RefreshToken expirado = refreshTokenValido();
        expirado.setDataExpiracao(LocalDateTime.now().minusHours(1));

        assertFalse(refreshTokenService.validarRefreshToken(expirado));
    }

    @Test
    void validarRefreshToken_revogado_retornaFalse() {
        RefreshToken revogado = refreshTokenValido();
        revogado.setRevogado(true);

        assertFalse(refreshTokenService.validarRefreshToken(revogado));
    }

    @Test
    void validarRefreshToken_valido_retornaTrue() {
        assertTrue(refreshTokenService.validarRefreshToken(refreshTokenValido()));
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        return usuario;
    }

    private RefreshToken refreshTokenValido() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(TOKEN);
        refreshToken.setUsuario(usuarioAtivo());
        refreshToken.setDataExpiracao(LocalDateTime.now().plusDays(1));
        refreshToken.setRevogado(false);
        return refreshToken;
    }
}
