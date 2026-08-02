package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.application.CargoService;
import br.com.techne.sistemafolha.cadastros.domain.CargoNotFoundException;
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

@WebMvcTest(controllers = CargoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class CargoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CargoService cargoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarTodos_semAuth_retorna401() throws Exception {
        mockMvc.perform(get("/cargos"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarTodos_retorna200() throws Exception {
        CargoDTO dto = new CargoDTO(1L, "Analista", true);
        when(cargoService.listarTodos()).thenReturn(List.of(dto));

        mockMvc.perform(get("/cargos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].descricao").value("Analista"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_retorna200() throws Exception {
        CargoDTO dto = new CargoDTO(1L, "Analista", true);
        when(cargoService.buscarPorId(1L)).thenReturn(dto);

        mockMvc.perform(get("/cargos/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_inexistente_retorna500() throws Exception {
        when(cargoService.buscarPorId(99L)).thenThrow(new CargoNotFoundException(99L));

        mockMvc.perform(get("/cargos/99"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_bodyValido_retorna200() throws Exception {
        CargoDTO dto = new CargoDTO(1L, "Analista", true);
        when(cargoService.cadastrar(any())).thenReturn(dto);

        mockMvc.perform(post("/cargos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Analista\",\"ativo\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descricao").value("Analista"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void cadastrar_descricaoVazia_retorna400() throws Exception {
        mockMvc.perform(post("/cargos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"\",\"ativo\":true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_bodyValido_retorna200() throws Exception {
        CargoDTO dto = new CargoDTO(1L, "Analista Sr", true);
        when(cargoService.atualizar(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(put("/cargos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"descricao\":\"Analista Sr\",\"ativo\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.descricao").value("Analista Sr"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_retorna204() throws Exception {
        mockMvc.perform(delete("/cargos/1"))
            .andExpect(status().isNoContent());

        verify(cargoService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_inexistente_retorna500() throws Exception {
        doThrow(new CargoNotFoundException(99L)).when(cargoService).remover(99L);

        mockMvc.perform(delete("/cargos/99"))
            .andExpect(status().isInternalServerError());
    }
}
