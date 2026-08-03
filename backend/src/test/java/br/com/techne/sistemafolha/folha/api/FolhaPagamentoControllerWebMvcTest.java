package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.folha.application.FolhaPagamentoService;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FolhaPagamentoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FolhaPagamentoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FolhaPagamentoService folhaPagamentoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void consultarPorFuncionario_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/folha-pagamento/funcionario/1")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorFuncionario_retorna200() throws Exception {
        when(folhaPagamentoService.consultarPorFuncionario(
            eq("user"), eq(1L), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31")), isNull()))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/folha-pagamento/funcionario/1")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorCentroCusto_retorna200() throws Exception {
        when(folhaPagamentoService.consultarPorCentroCusto(
            eq("user"), eq(2L), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31"))))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/folha-pagamento/centro-custo/2")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorLinhaNegocio_retorna200() throws Exception {
        when(folhaPagamentoService.consultarPorLinhaNegocio(
            eq("user"), eq(3L), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31"))))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/folha-pagamento/linha-negocio/3")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarPorPeriodo_retorna200() throws Exception {
        when(folhaPagamentoService.consultarPorPeriodo(
            eq("user"), isNull(), isNull(), isNull()))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/folha-pagamento"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void consultarTotaisPorFuncionario_retorna200() throws Exception {
        when(folhaPagamentoService.consultarTotaisPorFuncionario(
            eq("user"), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31")), eq(true)))
            .thenReturn(List.of());

        mockMvc.perform(get("/folha-pagamento/totais-funcionarios")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31")
                .param("decimoTerceiro", "true"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void remover_autorizado_retorna204() throws Exception {
        when(folhaPagamentoService.removerSeAutorizado("user", 1L)).thenReturn(true);

        mockMvc.perform(delete("/folha-pagamento/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void remover_naoAutorizado_retorna404() throws Exception {
        when(folhaPagamentoService.removerSeAutorizado("user", 99L)).thenReturn(false);

        mockMvc.perform(delete("/folha-pagamento/99"))
            .andExpect(status().isNotFound());
    }
}
