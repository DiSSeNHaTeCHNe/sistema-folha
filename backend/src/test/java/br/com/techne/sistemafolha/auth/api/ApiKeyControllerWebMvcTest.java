package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiKeyController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ApiKeyControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user")
    void postApiKeys_semPermissaoApiKey_retorna403() throws Exception {
        Usuario usuario = usuarioSemPermissaoApiKey();
        when(apiKeyService.resolverUsuarioPorLogin("user")).thenReturn(usuario);
        when(apiKeyService.criar(eq(usuario), any(ApiKeyCreateRequest.class)))
                .thenThrow(new AccessDeniedException("Permissão API_KEY necessária"));

        mockMvc.perform(post("/auth/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Integração\",\"diasValidade\":30}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Acesso negado"));
    }

    @Test
    @WithMockUser(username = "apiuser")
    void postApiKeys_nomeVazio_retorna400() throws Exception {
        when(apiKeyService.resolverUsuarioPorLogin("apiuser")).thenReturn(usuarioComPermissaoApiKey());

        mockMvc.perform(post("/auth/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"diasValidade\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "apiuser")
    void postApiKeys_diasValidadeInvalidos_retorna400() throws Exception {
        when(apiKeyService.resolverUsuarioPorLogin("apiuser")).thenReturn(usuarioComPermissaoApiKey());

        mockMvc.perform(post("/auth/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Key\",\"diasValidade\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/auth/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Key\",\"diasValidade\":366}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "apiuser")
    void postApiKeys_bodyValido_retorna201ComEscopoRead() throws Exception {
        Usuario usuario = usuarioComPermissaoApiKey();
        when(apiKeyService.resolverUsuarioPorLogin("apiuser")).thenReturn(usuario);
        LocalDateTime agora = LocalDateTime.now();
        when(apiKeyService.criar(eq(usuario), any(ApiKeyCreateRequest.class))).thenReturn(
                new ApiKeyCreatedDTO(
                        1L,
                        "Integração",
                        "sf_live_abc12345",
                        "sf_live_abc12345secret",
                        agora.plusDays(30),
                        "READ",
                        agora
                ));

        mockMvc.perform(post("/auth/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Integração\",\"diasValidade\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Integração"))
                .andExpect(jsonPath("$.prefixo").value("sf_live_abc12345"))
                .andExpect(jsonPath("$.chave").value("sf_live_abc12345secret"))
                .andExpect(jsonPath("$.escopo").value("READ"))
                .andExpect(jsonPath("$.dataExpiracao").exists())
                .andExpect(jsonPath("$.dataCriacao").exists());
    }

    @Test
    @WithMockUser(username = "apiuser")
    void getApiKeys_retorna200SemCampoChave() throws Exception {
        Usuario usuario = usuarioComPermissaoApiKey();
        when(apiKeyService.resolverUsuarioPorLogin("apiuser")).thenReturn(usuario);
        LocalDateTime agora = LocalDateTime.now();
        when(apiKeyService.listar(eq(usuario), eq(null))).thenReturn(List.of(
                new ApiKeyListDTO(
                        10L,
                        "Key teste",
                        "sf_live_abc12345",
                        agora.plusDays(30),
                        false,
                        "READ",
                        agora,
                        agora
                )
        ));

        mockMvc.perform(get("/auth/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Key teste"))
                .andExpect(jsonPath("$[0].prefixo").value("sf_live_abc12345"))
                .andExpect(jsonPath("$[0].escopo").value("READ"))
                .andExpect(jsonPath("$[0].chave").doesNotExist());
    }

    private Usuario usuarioComPermissaoApiKey() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setLogin("apiuser");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("API_KEY"));
        return usuario;
    }

    private Usuario usuarioSemPermissaoApiKey() {
        Usuario usuario = new Usuario();
        usuario.setId(11L);
        usuario.setLogin("user");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("ACESSO_TOTAL"));
        return usuario;
    }
}
