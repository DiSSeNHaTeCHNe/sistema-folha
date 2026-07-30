package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.ApiKeyCreateRequest;
import br.com.techne.sistemafolha.auth.api.ApiKeyCreatedDTO;
import br.com.techne.sistemafolha.auth.domain.ApiKey;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
}
