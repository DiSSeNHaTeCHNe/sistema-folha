package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.application.LinhaNegocioService;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocioNotFoundException;
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

@WebMvcTest(controllers = LinhaNegocioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LinhaNegocioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LinhaNegocioService linhaNegocioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarTodos_semAuth_retorna401() throws Exception {
        mockMvc.perform(get("/linhas-negocio"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarTodos_retorna200() throws Exception {
        LinhaNegocioDTO dto = new LinhaNegocioDTO(1L, "Tecnologia", true);
        when(linhaNegocioService.listarTodas()).thenReturn(List.of(dto));

        mockMvc.perform(get("/linhas-negocio"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].descricao").value("Tecnologia"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_encontrado_retorna200() throws Exception {
        when(linhaNegocioService.buscarPorId(1L)).thenReturn(new LinhaNegocioDTO(1L, "Tecnologia", true));

        mockMvc.perform(get("/linhas-negocio/1"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_naoEncontrado_retorna404() throws Exception {
        when(linhaNegocioService.buscarPorId(99L)).thenThrow(new LinhaNegocioNotFoundException(99L));

        mockMvc.perform(get("/linhas-negocio/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_sucesso_retorna200() throws Exception {
        when(linhaNegocioService.cadastrar(any())).thenReturn(new LinhaNegocioDTO(1L, "Tecnologia", true));

        mockMvc.perform(post("/linhas-negocio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Tecnologia\",\"ativo\":true}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_regraInvalida_retorna400() throws Exception {
        when(linhaNegocioService.cadastrar(any())).thenThrow(new IllegalArgumentException("Duplicado"));

        mockMvc.perform(post("/linhas-negocio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Tecnologia\",\"ativo\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_descricaoVazia_retorna400() throws Exception {
        mockMvc.perform(post("/linhas-negocio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"\",\"ativo\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_sucesso_retorna200() throws Exception {
        when(linhaNegocioService.atualizar(eq(1L), any()))
            .thenReturn(new LinhaNegocioDTO(1L, "Tecnologia Atualizada", true));

        mockMvc.perform(put("/linhas-negocio/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Tecnologia Atualizada\",\"ativo\":true}"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_naoEncontrado_retorna404() throws Exception {
        when(linhaNegocioService.atualizar(eq(99L), any()))
            .thenThrow(new LinhaNegocioNotFoundException(99L));

        mockMvc.perform(put("/linhas-negocio/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Tecnologia\",\"ativo\":true}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_regraInvalida_retorna400() throws Exception {
        when(linhaNegocioService.atualizar(eq(1L), any()))
            .thenThrow(new IllegalArgumentException("Duplicado"));

        mockMvc.perform(put("/linhas-negocio/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Tecnologia\",\"ativo\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/linhas-negocio/1"))
            .andExpect(status().isNoContent());

        verify(linhaNegocioService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_naoEncontrado_retorna404() throws Exception {
        doThrow(new LinhaNegocioNotFoundException(99L)).when(linhaNegocioService).remover(99L);

        mockMvc.perform(delete("/linhas-negocio/99"))
            .andExpect(status().isNotFound());
    }
}
