package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.cadastros.application.FuncionarioRubricaFixaService;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaNotFoundException;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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

@WebMvcTest(controllers = FuncionarioRubricaFixaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FuncionarioRubricaFixaControllerWebMvcTest {

    private static final String BODY_VALIDO = """
        {
          "funcionarioId": 1,
          "rubricaId": 2,
          "valor": 1500.00,
          "vigenciaInicio": "2024-01-01",
          "ativo": true
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FuncionarioRubricaFixaService funcionarioRubricaFixaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listar_semAuth_retorna401() throws Exception {
        mockMvc.perform(get("/funcionario-rubrica-fixa"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listar_comFiltros_retorna200() throws Exception {
        when(funcionarioRubricaFixaService.listar(1L, 2L)).thenReturn(List.of());

        mockMvc.perform(get("/funcionario-rubrica-fixa")
                .param("funcionarioId", "1")
                .param("rubricaId", "2"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_encontrado_retorna200() throws Exception {
        when(funcionarioRubricaFixaService.buscarPorId(1L)).thenReturn(dtoExemplo());

        mockMvc.perform(get("/funcionario-rubrica-fixa/1"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void buscarPorId_naoEncontrado_retorna404() throws Exception {
        when(funcionarioRubricaFixaService.buscarPorId(99L))
            .thenThrow(new FuncionarioRubricaFixaNotFoundException(99L));

        mockMvc.perform(get("/funcionario-rubrica-fixa/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void criar_sucesso_retorna200() throws Exception {
        when(funcionarioRubricaFixaService.criar(any())).thenReturn(dtoExemplo());

        mockMvc.perform(post("/funcionario-rubrica-fixa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void criar_regraInvalida_retorna400() throws Exception {
        when(funcionarioRubricaFixaService.criar(any()))
            .thenThrow(new IllegalArgumentException("Vigência inválida"));

        mockMvc.perform(post("/funcionario-rubrica-fixa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void criar_semRubrica_retorna400() throws Exception {
        mockMvc.perform(post("/funcionario-rubrica-fixa")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"funcionarioId\":1,\"valor\":100,\"vigenciaInicio\":\"2024-01-01\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_sucesso_retorna200() throws Exception {
        when(funcionarioRubricaFixaService.atualizar(eq(1L), any())).thenReturn(dtoExemplo());

        mockMvc.perform(put("/funcionario-rubrica-fixa/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_naoEncontrado_retorna404() throws Exception {
        when(funcionarioRubricaFixaService.atualizar(eq(99L), any()))
            .thenThrow(new FuncionarioRubricaFixaNotFoundException(99L));

        mockMvc.perform(put("/funcionario-rubrica-fixa/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_regraInvalida_retorna400() throws Exception {
        when(funcionarioRubricaFixaService.atualizar(eq(1L), any()))
            .thenThrow(new IllegalArgumentException("Vigência inválida"));

        mockMvc.perform(put("/funcionario-rubrica-fixa/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/funcionario-rubrica-fixa/1"))
            .andExpect(status().isNoContent());

        verify(funcionarioRubricaFixaService).remover(1L);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_naoEncontrado_retorna404() throws Exception {
        doThrow(new FuncionarioRubricaFixaNotFoundException(99L))
            .when(funcionarioRubricaFixaService).remover(99L);

        mockMvc.perform(delete("/funcionario-rubrica-fixa/99"))
            .andExpect(status().isNotFound());
    }

    private FuncionarioRubricaFixaDTO dtoExemplo() {
        return new FuncionarioRubricaFixaDTO(
            1L, 1L, 2L, new BigDecimal("1500.00"),
            LocalDate.of(2024, 1, 1), null, null, true,
            "João", "R001", "Rubrica teste", 100.0);
    }
}
