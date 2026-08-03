package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.domain.RefreshToken;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.RefreshTokenRepository;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.security.JwtService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void criarRefreshToken_comClockFixo_expiracaoValidaAntesEExpiradaDepois() {
        ZoneId zone = ZoneId.systemDefault();
        Instant base = Instant.parse("2024-06-01T12:00:00Z");
        Clock clockAntes = Clock.fixed(base, zone);
        RefreshTokenService serviceA = new RefreshTokenService(
            refreshTokenRepository, usuarioRepository, jwtService, clockAntes);

        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(jwtService.generateRefreshToken()).thenReturn(TOKEN);
        when(jwtService.getRefreshExpirationTime()).thenReturn(86_400_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = serviceA.criarRefreshToken(LOGIN);

        assertTrue(serviceA.validarRefreshToken(token));

        Clock clockDepois = Clock.fixed(base.plusSeconds(86401), zone);
        RefreshTokenService serviceB = new RefreshTokenService(
            refreshTokenRepository, usuarioRepository, jwtService, clockDepois);

        assertFalse(serviceB.validarRefreshToken(token));
    }

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

    @Test
    void buscarPorToken_naoLogaValorDoToken() {
        when(refreshTokenRepository.findByToken(TOKEN)).thenReturn(Optional.empty());
        ListAppender<ILoggingEvent> appender = capturarLogsRefreshTokenService();

        refreshTokenService.buscarPorToken(TOKEN);

        assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains(TOKEN)));
    }

    @Test
    void validarRefreshToken_invalido_naoLogaValorDoToken() {
        RefreshToken revogado = refreshTokenValido();
        revogado.setRevogado(true);
        ListAppender<ILoggingEvent> appender = capturarLogsRefreshTokenService();

        refreshTokenService.validarRefreshToken(revogado);

        assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains(TOKEN)));
    }

    private ListAppender<ILoggingEvent> capturarLogsRefreshTokenService() {
        Logger logger = (Logger) LoggerFactory.getLogger(RefreshTokenService.class);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
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
