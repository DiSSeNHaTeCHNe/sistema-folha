package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import br.com.techne.sistemafolha.workspace.application.WidgetQueryService;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceWidgetDataController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WorkspaceWidgetDataControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WidgetQueryService widgetQueryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void obterDados_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_semEscopo_retorna403() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("abc"), any()))
            .thenThrow(new WorkspaceAcessoNegadoException());

        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_kpi_retorna200ComMoedaPtBr() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("abc"), any()))
            .thenReturn(new WorkspaceWidgetDataDTO(
                "abc", 10L, null, "KPI", false, false, "2025-06",
                Map.of("valor", "R$ 1.234,56"), List.of()));

        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valores.valor").value("R$ 1.234,56"))
            .andExpect(jsonPath("$.semDados").value(false));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_semDados_retorna200() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("abc"), any()))
            .thenReturn(WorkspaceWidgetDataDTO.semDados("abc", 10L, null, "KPI", "2025-06"));

        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.semDados").value(true));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_comCompetencia_propagaParametro() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("abc"), any()))
            .thenReturn(new WorkspaceWidgetDataDTO(
                "abc", 10L, null, "TABELA", false, false, "2025-03",
                Map.of(), List.of(Map.of("orcado", "R$ 100,00"))));

        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data?competencia=2025-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.competencia").value("2025-03"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_widgetInvalido_retorna200ComFlag() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("abc"), any()))
            .thenReturn(new WorkspaceWidgetDataDTO(
                "abc", 10L, null, "KPI", true, true, "2025-06", Map.of(), List.of()));

        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.invalido").value(true));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_tabela_retornaLinhas() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("tbl"), any()))
            .thenReturn(new WorkspaceWidgetDataDTO(
                "tbl", 11L, null, "TABELA", false, false, "2025-06",
                Map.of(),
                List.of(Map.of(
                    "centro_custo", "CC A",
                    "orcado", "R$ 10.000,00",
                    "realizado", "R$ 8.000,00"))));

        mockMvc.perform(get("/workspace/workspaces/1/widgets/tbl/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.linhas[0].realizado").value("R$ 8.000,00"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void obterDados_erroValidacao_retorna400() throws Exception {
        when(widgetQueryService.obterDados(eq("user-a"), eq(1L), eq("abc"), any()))
            .thenThrow(new IllegalArgumentException("Widget não encontrado no layout: abc"));

        mockMvc.perform(get("/workspace/workspaces/1/widgets/abc/data"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }
}
