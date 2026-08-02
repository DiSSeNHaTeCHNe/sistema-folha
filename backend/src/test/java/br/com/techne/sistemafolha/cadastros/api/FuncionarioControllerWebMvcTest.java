package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.application.FuncionarioService;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioJaExisteException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FuncionarioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FuncionarioControllerWebMvcTest {

    private static final String BODY_VALIDO = """
        {
          "nome": "João Silva",
          "cpf": "12345678901",
          "dataAdmissao": "2024-01-15",
          "cargoId": 1,
          "centroCustoId": 1,
          "idExterno": "MAT001",
          "ativo": true
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuncionarioService funcionarioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listar_semAuth_retorna401() throws Exception {
        mockMvc.perform(get("/funcionarios"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void listar_comFiltros_delegaAoService() throws Exception {
        when(funcionarioService.listarParaUsuario(
            eq("gestor"), eq("João"), eq(1L), eq(2L), eq(3L), eq(FuncionarioStatusFiltro.TODOS)))
            .thenReturn(java.util.List.of());

        mockMvc.perform(get("/funcionarios")
                .param("nome", "João")
                .param("cargoId", "1")
                .param("centroCustoId", "2")
                .param("linhaNegocioId", "3")
                .param("status", "TODOS"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void buscarPorId_encontrado_retorna200() throws Exception {
        FuncionarioDTO dto = funcionarioExemplo();
        when(funcionarioService.buscarPorIdParaUsuario("gestor", 1L)).thenReturn(dto);

        mockMvc.perform(get("/funcionarios/1"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void buscarPorId_naoEncontrado_retorna404() throws Exception {
        when(funcionarioService.buscarPorIdParaUsuario("gestor", 99L))
            .thenThrow(new FuncionarioNotFoundException(99L));

        mockMvc.perform(get("/funcionarios/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_sucesso_retorna200() throws Exception {
        when(funcionarioService.cadastrar(any())).thenReturn(funcionarioExemplo());

        mockMvc.perform(post("/funcionarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_duplicado_retorna400() throws Exception {
        when(funcionarioService.cadastrar(any()))
            .thenThrow(new FuncionarioJaExisteException("12345678901"));

        mockMvc.perform(post("/funcionarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_nomeVazio_retorna400() throws Exception {
        mockMvc.perform(post("/funcionarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"\",\"cpf\":\"12345678901\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_sucesso_retorna200() throws Exception {
        when(funcionarioService.atualizar(eq(1L), any())).thenReturn(funcionarioExemplo());

        mockMvc.perform(put("/funcionarios/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_naoEncontrado_retorna404() throws Exception {
        when(funcionarioService.atualizar(eq(99L), any()))
            .thenThrow(new FuncionarioNotFoundException(99L));

        mockMvc.perform(put("/funcionarios/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/funcionarios/1"))
            .andExpect(status().isNoContent());

        verify(funcionarioService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_naoEncontrado_retorna404() throws Exception {
        doThrow(new FuncionarioNotFoundException(99L)).when(funcionarioService).remover(99L);

        mockMvc.perform(delete("/funcionarios/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void listar_statusDefaultAtivo() throws Exception {
        when(funcionarioService.listarParaUsuario(
            eq("gestor"), isNull(), isNull(), isNull(), isNull(), eq(FuncionarioStatusFiltro.ATIVO)))
            .thenReturn(java.util.List.of());

        mockMvc.perform(get("/funcionarios"))
            .andExpect(status().isOk());
    }

    private FuncionarioDTO funcionarioExemplo() {
        return new FuncionarioDTO(
            1L, "João Silva", "12345678901", LocalDate.of(2024, 1, 15),
            1L, "Analista", 1L, "Dev", 1L, "TI", "MAT001", true);
    }
}
