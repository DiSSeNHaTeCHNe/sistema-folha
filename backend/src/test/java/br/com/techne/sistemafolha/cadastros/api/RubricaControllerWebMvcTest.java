package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.application.RubricaService;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
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

@WebMvcTest(controllers = RubricaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RubricaControllerWebMvcTest {

    private static final String BODY_VALIDO = """
        {
          "codigo": "R001",
          "descricao": "Salário Base",
          "tipo": "PROVENTO",
          "ativo": true
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RubricaService rubricaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listar_semAuth_retorna401() throws Exception {
        mockMvc.perform(get("/rubricas"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listar_comFiltros_retorna200() throws Exception {
        when(rubricaService.listar("R001", "Sal", RubricaStatusFiltro.INATIVO)).thenReturn(List.of());

        mockMvc.perform(get("/rubricas")
                .param("codigo", "R001")
                .param("descricao", "Sal")
                .param("status", "INATIVO"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listar_statusDefaultAtivo() throws Exception {
        when(rubricaService.listar(null, null, RubricaStatusFiltro.ATIVO)).thenReturn(List.of());

        mockMvc.perform(get("/rubricas"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_encontrado_retorna200() throws Exception {
        when(rubricaService.buscarPorId(1L)).thenReturn(rubricaExemplo());

        mockMvc.perform(get("/rubricas/1"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_naoEncontrado_retorna404() throws Exception {
        when(rubricaService.buscarPorId(99L)).thenThrow(new RubricaNotFoundException("Rubrica 99 não encontrada"));

        mockMvc.perform(get("/rubricas/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_sucesso_retorna200() throws Exception {
        when(rubricaService.cadastrar(any())).thenReturn(rubricaExemplo());

        mockMvc.perform(post("/rubricas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_regraInvalida_retorna400() throws Exception {
        when(rubricaService.cadastrar(any())).thenThrow(new IllegalArgumentException("Código duplicado"));

        mockMvc.perform(post("/rubricas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_sucesso_retorna200() throws Exception {
        when(rubricaService.atualizar(eq(1L), any())).thenReturn(rubricaExemplo());

        mockMvc.perform(put("/rubricas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_naoEncontrado_retorna404() throws Exception {
        when(rubricaService.atualizar(eq(99L), any())).thenThrow(new RubricaNotFoundException("Rubrica 99 não encontrada"));

        mockMvc.perform(put("/rubricas/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_regraInvalida_retorna400() throws Exception {
        when(rubricaService.atualizar(eq(1L), any())).thenThrow(new IllegalArgumentException("Inválido"));

        mockMvc.perform(put("/rubricas/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/rubricas/1"))
            .andExpect(status().isNoContent());

        verify(rubricaService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_naoEncontrado_retorna404() throws Exception {
        doThrow(new RubricaNotFoundException("Rubrica 99 não encontrada")).when(rubricaService).remover(99L);

        mockMvc.perform(delete("/rubricas/99"))
            .andExpect(status().isNotFound());
    }

    private RubricaDTO rubricaExemplo() {
        return new RubricaDTO(1L, "R001", "Salário Base", null, "PROVENTO",
            null, null, null, null, true);
    }
}
