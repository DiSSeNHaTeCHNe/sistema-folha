package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.ApiKeyCreateRequest;
import br.com.techne.sistemafolha.auth.api.ApiKeyCreatedDTO;
import br.com.techne.sistemafolha.auth.api.ApiKeyListDTO;
import br.com.techne.sistemafolha.auth.domain.ApiKey;
import br.com.techne.sistemafolha.auth.domain.ApiKeyNotFoundException;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.ApiKeyRepository;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ApiKeyService apiKeyService;

    @Test
    void criar_comPermissaoApiKey_persisteEscopoReadHashDiferenteDaChave() {
        Usuario usuario = usuarioComPermissaoApiKey();
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey key = inv.getArgument(0);
            key.setId(1L);
            key.setDataCriacao(LocalDateTime.now());
            return key;
        });

        ApiKeyCreatedDTO result = apiKeyService.criar(usuario, new ApiKeyCreateRequest("Integração", 30));

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey salva = captor.getValue();

        assertEquals(ApiKey.ESCOPO_READ, salva.getEscopo());
        assertEquals("$2a$10$hashed", salva.getHashChave());
        assertNotEquals(result.chave(), salva.getHashChave());
        assertEquals(ApiKey.ESCOPO_READ, result.escopo());
        assertTrue(result.chave().startsWith(ApiKeyService.CHAVE_PREFIX));
        assertEquals("Integração", result.nome());
        assertNotNull(result.dataExpiracao());
    }

    @Test
    void criar_semPermissaoApiKey_lancaAccessDeniedException() {
        Usuario usuario = usuarioSemPermissaoApiKey();

        assertThrows(AccessDeniedException.class,
                () -> apiKeyService.criar(usuario, new ApiKeyCreateRequest("Integração", 30)));
    }

    @Test
    void criar_diasValidadeInvalidos_lancaIllegalArgumentException() {
        Usuario usuario = usuarioComPermissaoApiKey();

        assertThrows(IllegalArgumentException.class,
                () -> apiKeyService.criar(usuario, new ApiKeyCreateRequest("Integração", 0)));
        assertThrows(IllegalArgumentException.class,
                () -> apiKeyService.criar(usuario, new ApiKeyCreateRequest("Integração", 366)));
    }

    @Test
    void criar_chaveTemPrefixoSfLiveEEntropiaMinima() {
        Usuario usuario = usuarioComPermissaoApiKey();
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> inv.getArgument(0));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey key = inv.getArgument(0);
            key.setId(2L);
            key.setDataCriacao(LocalDateTime.now());
            return key;
        });

        ApiKeyCreatedDTO result = apiKeyService.criar(usuario, new ApiKeyCreateRequest("Agente", null));

        assertTrue(result.chave().startsWith(ApiKeyService.CHAVE_PREFIX));
        String secretPart = result.chave().substring(ApiKeyService.CHAVE_PREFIX.length() + ApiKeyService.PREFIXO_RANDOM_CHARS);
        assertFalse(secretPart.isEmpty());
        assertTrue(secretPart.length() >= 43);
    }

    @Test
    void criar_diasValidadeOmitido_usaDefault365() {
        Usuario usuario = usuarioComPermissaoApiKey();
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey key = inv.getArgument(0);
            key.setId(3L);
            key.setDataCriacao(LocalDateTime.now());
            return key;
        });

        ApiKeyCreatedDTO result = apiKeyService.criar(usuario, new ApiKeyCreateRequest("Default", null));

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey salva = captor.getValue();
        assertTrue(salva.getDataExpiracao().isAfter(LocalDateTime.now().plusDays(364)));
        assertTrue(salva.getDataExpiracao().isBefore(LocalDateTime.now().plusDays(366)));
        assertNotNull(result.dataExpiracao());
    }

    @Test
    void listar_propriasKeys_retornaMetadadosSemSecret() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 100L);
        when(apiKeyRepository.findByUsuarioIdOrderByDataCriacaoDesc(10L)).thenReturn(List.of(apiKey));

        List<ApiKeyListDTO> result = apiKeyService.listar(usuario, null);

        assertEquals(1, result.size());
        ApiKeyListDTO dto = result.get(0);
        assertEquals(100L, dto.id());
        assertEquals("Key teste", dto.nome());
        assertEquals(ApiKey.ESCOPO_READ, dto.escopo());
        assertEquals("sf_live_abc12345", dto.prefixo());
    }

    @Test
    void listar_naoAdminTentaOutroUsuario_lancaApiKeyNotFoundException() {
        Usuario usuario = usuarioComPermissaoApiKey();

        assertThrows(ApiKeyNotFoundException.class, () -> apiKeyService.listar(usuario, 99L));
    }

    @Test
    void listar_adminPorUsuarioId_retornaKeysDoUsuario() {
        Usuario admin = usuarioAdmin();
        Usuario outro = usuarioOutro();
        ApiKey apiKey = apiKeyDoUsuario(outro, 200L);
        when(apiKeyRepository.findByUsuarioIdOrderByDataCriacaoDesc(20L)).thenReturn(List.of(apiKey));

        List<ApiKeyListDTO> result = apiKeyService.listar(admin, 20L);

        assertEquals(1, result.size());
        assertEquals(200L, result.get(0).id());
    }

    @Test
    void revogar_keyPropria_marcaRevogado() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 300L);
        when(apiKeyRepository.findById(300L)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(apiKey)).thenReturn(apiKey);

        apiKeyService.revogar(usuario, 300L);

        assertTrue(apiKey.isRevogado());
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void revogar_keyJaRevogada_eIdempotente() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 301L);
        apiKey.setRevogado(true);
        when(apiKeyRepository.findById(301L)).thenReturn(Optional.of(apiKey));

        apiKeyService.revogar(usuario, 301L);

        verify(apiKeyRepository, never()).save(apiKey);
    }

    @Test
    void revogar_naoAdminKeyAlheia_lancaApiKeyNotFoundException() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuarioOutro(), 302L);
        when(apiKeyRepository.findById(302L)).thenReturn(Optional.of(apiKey));

        assertThrows(ApiKeyNotFoundException.class, () -> apiKeyService.revogar(usuario, 302L));
    }

    @Test
    void revogar_adminKeyDeOutroUsuario_marcaRevogado() {
        Usuario admin = usuarioAdmin();
        ApiKey apiKey = apiKeyDoUsuario(usuarioOutro(), 303L);
        when(apiKeyRepository.findById(303L)).thenReturn(Optional.of(apiKey));
        when(apiKeyRepository.save(apiKey)).thenReturn(apiKey);

        apiKeyService.revogar(admin, 303L);

        assertTrue(apiKey.isRevogado());
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void autenticarPorChave_keyValida_retornaUsuarioDono() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 400L);
        String chave = "sf_live_abc12345secretpart";
        when(apiKeyRepository.findByPrefixoAndRevogadoFalse("sf_live_abc12345")).thenReturn(Optional.of(apiKey));
        when(passwordEncoder.matches(chave, "hash")).thenReturn(true);

        Optional<Usuario> result = apiKeyService.autenticarPorChave(chave);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
    }

    @Test
    void autenticarPorChave_keyRevogada_retornaEmpty() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 401L);
        apiKey.setRevogado(true);
        String chave = "sf_live_abc12345secretpart";
        when(apiKeyRepository.findByPrefixoAndRevogadoFalse("sf_live_abc12345")).thenReturn(Optional.of(apiKey));

        Optional<Usuario> result = apiKeyService.autenticarPorChave(chave);

        assertTrue(result.isEmpty());
    }

    @Test
    void autenticarPorChave_keyExpirada_retornaEmpty() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 402L);
        apiKey.setDataExpiracao(LocalDateTime.now().minusDays(1));
        String chave = "sf_live_abc12345secretpart";
        when(apiKeyRepository.findByPrefixoAndRevogadoFalse("sf_live_abc12345")).thenReturn(Optional.of(apiKey));

        Optional<Usuario> result = apiKeyService.autenticarPorChave(chave);

        assertTrue(result.isEmpty());
    }

    @Test
    void autenticarPorChave_usuarioSemPermissaoApiKey_retornaEmpty() {
        Usuario usuario = usuarioSemPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 403L);
        String chave = "sf_live_abc12345secretpart";
        when(apiKeyRepository.findByPrefixoAndRevogadoFalse("sf_live_abc12345")).thenReturn(Optional.of(apiKey));
        when(passwordEncoder.matches(chave, "hash")).thenReturn(true);

        Optional<Usuario> result = apiKeyService.autenticarPorChave(chave);

        assertTrue(result.isEmpty());
    }

    @Test
    void autenticarPorChave_hashInvalido_retornaEmpty() {
        Usuario usuario = usuarioComPermissaoApiKey();
        ApiKey apiKey = apiKeyDoUsuario(usuario, 404L);
        String chave = "sf_live_abc12345wrongsecret";
        when(apiKeyRepository.findByPrefixoAndRevogadoFalse("sf_live_abc12345")).thenReturn(Optional.of(apiKey));
        when(passwordEncoder.matches(chave, "hash")).thenReturn(false);

        Optional<Usuario> result = apiKeyService.autenticarPorChave(chave);

        assertTrue(result.isEmpty());
    }

    @Test
    void autenticarPorChave_naoLogaSecret() {
        String chave = "sf_live_abc12345supersecretvalue";
        when(apiKeyRepository.findByPrefixoAndRevogadoFalse("sf_live_abc12345")).thenReturn(Optional.empty());
        ListAppender<ILoggingEvent> appender = capturarLogsApiKeyService();

        apiKeyService.autenticarPorChave(chave);

        assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains("supersecretvalue")));
    }

    private ListAppender<ILoggingEvent> capturarLogsApiKeyService() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiKeyService.class);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private Usuario usuarioComPermissaoApiKey() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setLogin("usuario.api");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of(ApiKeyService.PERMISSAO_API_KEY));
        return usuario;
    }

    private Usuario usuarioSemPermissaoApiKey() {
        Usuario usuario = new Usuario();
        usuario.setId(11L);
        usuario.setLogin("usuario.sem");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("ACESSO_TOTAL"));
        return usuario;
    }

    private Usuario usuarioAdmin() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("admin");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of(ApiKeyService.PERMISSAO_ADMIN));
        return usuario;
    }

    private Usuario usuarioOutro() {
        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setLogin("outro");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of(ApiKeyService.PERMISSAO_API_KEY));
        return usuario;
    }

    private ApiKey apiKeyDoUsuario(Usuario usuario, Long id) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setUsuario(usuario);
        apiKey.setNome("Key teste");
        apiKey.setPrefixo("sf_live_abc12345");
        apiKey.setHashChave("hash");
        apiKey.setEscopo(ApiKey.ESCOPO_READ);
        apiKey.setDataExpiracao(LocalDateTime.now().plusDays(10));
        apiKey.setRevogado(false);
        apiKey.setDataCriacao(LocalDateTime.now());
        return apiKey;
    }
}
