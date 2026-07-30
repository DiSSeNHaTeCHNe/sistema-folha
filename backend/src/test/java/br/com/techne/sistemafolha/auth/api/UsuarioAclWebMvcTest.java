package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.application.UsuarioService;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.domain.UsuarioNotFoundException;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UsuarioAclWebMvcTest {

    private static final String API_KEY_BEARER = "Bearer sf_live_testkey1234567890abcdefghij";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void listar_jwtComListaVazia_retorna200() throws Exception {
        when(usuarioService.listarParaUsuario(eq("gestor"), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/usuarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listar_bearerApiKeyComListaVazia_retorna200() throws Exception {
        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));
        when(usuarioService.listarParaUsuario(eq("gestor"), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/usuarios").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void listar_jwtEBearerApiKey_retornamMesmosDados() throws Exception {
        UsuarioDTO usuario = usuarioExemplo();
        when(usuarioService.listarParaUsuario(eq("gestor"), isNull(), isNull(), isNull()))
                .thenReturn(List.of(usuario));

        mockMvc.perform(get("/usuarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].login").value("operador"));

        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));

        mockMvc.perform(get("/usuarios").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].login").value("operador"));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void buscarPorId_foraEscopo_retorna404() throws Exception {
        when(usuarioService.buscarPorIdParaUsuario(eq("gestor"), eq(99L)))
                .thenThrow(new UsuarioNotFoundException(99L));

        mockMvc.perform(get("/usuarios/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void buscarPorId_bearerApiKey_foraEscopo_retorna404() throws Exception {
        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));
        when(usuarioService.buscarPorIdParaUsuario(eq("gestor"), eq(99L)))
                .thenThrow(new UsuarioNotFoundException(99L));

        mockMvc.perform(get("/usuarios/99").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isNotFound());
    }

    private Usuario usuarioGestor() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setLogin("gestor");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("GESTOR", "API_KEY"));
        return usuario;
    }

    private UsuarioDTO usuarioExemplo() {
        return new UsuarioDTO(1L, "operador", null, "Operador", List.of("USER"), 10L, "Operador", null);
    }
}
