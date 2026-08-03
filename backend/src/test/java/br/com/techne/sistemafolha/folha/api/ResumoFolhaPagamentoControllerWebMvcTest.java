package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.folha.application.ResumoFolhaPagamentoService;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResumoFolhaPagamentoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ResumoFolhaPagamentoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumoFolhaPagamentoService resumoFolhaPagamentoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarTodos_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/resumo-folha-pagamento"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void listarTodos_retorna200() throws Exception {
        when(resumoFolhaPagamentoService.listarTodos(eq("user"), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resumo-folha-pagamento"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void listarTodos_comAnoMes_retorna200() throws Exception {
        when(resumoFolhaPagamentoService.listarTodos("user", 2026, 3))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resumo-folha-pagamento")
                .param("ano", "2026")
                .param("mes", "3"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void listarTodos_anoInvalido_retornaErro() throws Exception {
        mockMvc.perform(get("/resumo-folha-pagamento").param("ano", "1999"))
            .andExpect(status().is5xxServerError());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorPeriodo_retorna200() throws Exception {
        when(resumoFolhaPagamentoService.consultarPorPeriodo(
            eq("user"), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31"))))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resumo-folha-pagamento/periodo")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorCompetencia_encontrado_retorna200() throws Exception {
        ResumoFolhaPagamentoDTO dto = new ResumoFolhaPagamentoDTO(
            1L, 10, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE,
            BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
            LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"),
            LocalDateTime.now(), false, true);
        when(resumoFolhaPagamentoService.consultarPorCompetencia(
            eq("user"), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31"))))
            .thenReturn(Optional.of(dto));

        mockMvc.perform(get("/resumo-folha-pagamento/competencia")
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorCompetencia_naoEncontrado_retorna404() throws Exception {
        when(resumoFolhaPagamentoService.consultarPorCompetencia(
            eq("user"), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31"))))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/resumo-folha-pagamento/competencia")
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void listarMaisRecentes_retorna200() throws Exception {
        when(resumoFolhaPagamentoService.listarMaisRecentes("user"))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/resumo-folha-pagamento/latest"))
            .andExpect(status().isOk());
    }
}
