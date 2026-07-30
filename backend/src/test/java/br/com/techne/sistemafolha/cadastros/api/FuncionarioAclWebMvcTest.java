package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.cadastros.application.FuncionarioService;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FuncionarioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FuncionarioAclWebMvcTest {

    private static final String API_KEY_BEARER = "Bearer sf_live_testkey1234567890abcdefghij";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuncionarioService funcionarioService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void listar_jwtComListaVazia_retorna200() throws Exception {
        when(funcionarioService.listarParaUsuario(
            eq("gestor"), isNull(), isNull(), isNull(), isNull(), eq(FuncionarioStatusFiltro.ATIVO)))
            .thenReturn(List.of());

        mockMvc.perform(get("/funcionarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listar_bearerApiKeyComListaVazia_retorna200() throws Exception {
        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));
        when(funcionarioService.listarParaUsuario(
            eq("gestor"), isNull(), isNull(), isNull(), isNull(), eq(FuncionarioStatusFiltro.ATIVO)))
            .thenReturn(List.of());

        mockMvc.perform(get("/funcionarios").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void listar_jwtEBearerApiKey_retornamMesmosDados() throws Exception {
        FuncionarioDTO funcionario = funcionarioExemplo();
        when(funcionarioService.listarParaUsuario(
            eq("gestor"), isNull(), isNull(), isNull(), isNull(), eq(FuncionarioStatusFiltro.ATIVO)))
            .thenReturn(List.of(funcionario));

        mockMvc.perform(get("/funcionarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].nome").value("João Silva"));

        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));

        mockMvc.perform(get("/funcionarios").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].nome").value("João Silva"));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void buscarPorId_foraEscopo_retorna404() throws Exception {
        when(funcionarioService.buscarPorIdParaUsuario(eq("gestor"), eq(99L)))
            .thenThrow(new FuncionarioNotFoundException(99L));

        mockMvc.perform(get("/funcionarios/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void buscarPorId_bearerApiKey_foraEscopo_retorna404() throws Exception {
        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));
        when(funcionarioService.buscarPorIdParaUsuario(eq("gestor"), eq(99L)))
            .thenThrow(new FuncionarioNotFoundException(99L));

        mockMvc.perform(get("/funcionarios/99").header("Authorization", API_KEY_BEARER))
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

    private FuncionarioDTO funcionarioExemplo() {
        return new FuncionarioDTO(
            1L,
            "João Silva",
            "12345678901",
            LocalDate.of(2024, 1, 15),
            1L,
            "Analista",
            793L,
            "Plugin",
            1L,
            "Software",
            "MAT001",
            true
        );
    }
}
