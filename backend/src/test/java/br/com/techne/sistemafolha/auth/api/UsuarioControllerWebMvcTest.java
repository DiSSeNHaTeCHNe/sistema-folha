package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.application.UsuarioService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UsuarioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UsuarioControllerWebMvcTest {

    private static final String BODY_VALIDO = """
        {
          "login": "novo.user",
          "nome": "Novo Usuário",
          "permissoes": ["USER"]
        }
        """;

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
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorFuncionario_encontrado_retorna200() throws Exception {
        UsuarioDTO dto = new UsuarioDTO(1L, "user", null, "User", List.of("USER"), 10L, "Func", null);
        when(usuarioService.buscarPorFuncionarioParaUsuario("admin", 10L)).thenReturn(dto);

        mockMvc.perform(get("/usuarios/funcionario/10"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorFuncionario_naoEncontrado_retorna404() throws Exception {
        when(usuarioService.buscarPorFuncionarioParaUsuario("admin", 99L)).thenReturn(null);

        mockMvc.perform(get("/usuarios/funcionario/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_bodyValido_retorna200() throws Exception {
        UsuarioDTO dto = new UsuarioDTO(1L, "novo.user", null, "Novo Usuário", List.of("USER"), null, null, null);
        when(usuarioService.cadastrar(any())).thenReturn(dto);

        mockMvc.perform(post("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_bodyValido_retorna200() throws Exception {
        UsuarioDTO dto = new UsuarioDTO(1L, "novo.user", null, "Atualizado", List.of("USER"), null, null, null);
        when(usuarioService.atualizar(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/usuarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/usuarios/1"))
            .andExpect(status().isNoContent());

        verify(usuarioService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void alterarSenha_sucesso_retorna200() throws Exception {
        mockMvc.perform(post("/usuarios/1/alterar-senha")
                .param("senhaAtual", "old")
                .param("novaSenha", "new123"))
            .andExpect(status().isOk());

        verify(usuarioService).alterarSenha(1L, "old", "new123");
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void alterarSenha_senhaIncorreta_retorna400() throws Exception {
        doThrow(new RuntimeException("Senha atual incorreta"))
            .when(usuarioService).alterarSenha(1L, "wrong", "new123");

        mockMvc.perform(post("/usuarios/1/alterar-senha")
                .param("senhaAtual", "wrong")
                .param("novaSenha", "new123"))
            .andExpect(status().isBadRequest());
    }
}
