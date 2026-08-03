package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.application.CentroCustoService;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CentroCustoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CentroCustoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CentroCustoService centroCustoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarTodos_semAuth_retorna401() throws Exception {
        mockMvc.perform(get("/centros-custo"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarTodos_retorna200() throws Exception {
        CentroCustoDTO dto = new CentroCustoDTO(1L, "Dev", true, 1L);
        when(centroCustoService.listarTodas()).thenReturn(List.of(dto));

        mockMvc.perform(get("/centros-custo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].descricao").value("Dev"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarPorLinhaNegocio_retorna200() throws Exception {
        when(centroCustoService.listarPorLinhaNegocio(1L)).thenReturn(List.of());

        mockMvc.perform(get("/centros-custo/linha-negocio/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_retorna200() throws Exception {
        CentroCustoDTO dto = new CentroCustoDTO(1L, "Dev", true, 1L);
        when(centroCustoService.buscarPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/centros-custo/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_inexistente_retorna404() throws Exception {
        when(centroCustoService.buscarPorId(99L)).thenThrow(new CentroCustoNotFoundException(99L));

        mockMvc.perform(get("/centros-custo/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_bodyValido_retorna200() throws Exception {
        CentroCustoDTO dto = new CentroCustoDTO(1L, "Dev", true, 1L);
        when(centroCustoService.cadastrar(any())).thenReturn(dto);

        mockMvc.perform(post("/centros-custo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Dev\",\"ativo\":true,\"linhaNegocioId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descricao").value("Dev"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_semLinhaNegocio_retorna400() throws Exception {
        mockMvc.perform(post("/centros-custo")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Dev\",\"ativo\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_bodyValido_retorna200() throws Exception {
        CentroCustoDTO dto = new CentroCustoDTO(1L, "Dev Atualizado", true, 1L);
        when(centroCustoService.atualizar(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/centros-custo/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Dev Atualizado\",\"ativo\":true,\"linhaNegocioId\":1}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_retorna204() throws Exception {
        mockMvc.perform(delete("/centros-custo/1"))
            .andExpect(status().isNoContent());

        verify(centroCustoService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_inexistente_retorna404() throws Exception {
        doThrow(new CentroCustoNotFoundException(99L)).when(centroCustoService).remover(99L);

        mockMvc.perform(delete("/centros-custo/99"))
            .andExpect(status().isNotFound());
    }
}
