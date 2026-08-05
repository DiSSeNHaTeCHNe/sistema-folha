package br.com.techne.sistemafolha.dashboard.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.dashboard.application.DashboardWidgetQueryService;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardWidgetController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardWidgetControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardWidgetQueryService dashboardWidgetQueryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void getWidgetData_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/dashboard/widgets/kpi-total-funcionarios/data"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void getWidgetData_semEscopo_retorna403() throws Exception {
        when(dashboardWidgetQueryService.consultar(eq("user-a"), eq("kpi-total-funcionarios"), any()))
            .thenThrow(new DashboardAcessoNegadoException());

        mockMvc.perform(get("/dashboard/widgets/kpi-total-funcionarios/data"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void getWidgetData_widgetForaDoCatalogo_retorna403() throws Exception {
        when(dashboardWidgetQueryService.consultar(eq("user-a"), eq("grafico-funcionarios-por-cargo"), any()))
            .thenThrow(new DashboardAcessoNegadoException());

        mockMvc.perform(get("/dashboard/widgets/grafico-funcionarios-por-cargo/data"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "scoped", roles = "USER")
    void getWidgetData_centroCustoForaDoEscopo_retorna403() throws Exception {
        when(dashboardWidgetQueryService.consultar(eq("scoped"), eq("kpi-total-funcionarios"), any()))
            .thenThrow(new DashboardAcessoNegadoException());

        mockMvc.perform(get("/dashboard/widgets/kpi-total-funcionarios/data")
                .param("centroCustoId", "99"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void getWidgetData_comEscopoTotal_retorna200() throws Exception {
        WidgetDataDTO dto = new WidgetDataDTO(
            "kpi-total-funcionarios", "2024-06", false,
            42L, null, null, null, null, null, null, null, null, null, null);
        when(dashboardWidgetQueryService.consultar(eq("gestor"), eq("kpi-total-funcionarios"), any()))
            .thenReturn(dto);

        mockMvc.perform(get("/dashboard/widgets/kpi-total-funcionarios/data"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.widgetId").value("kpi-total-funcionarios"))
            .andExpect(jsonPath("$.totalFuncionarios").value(42))
            .andExpect(jsonPath("$.semDados").value(false));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void getWidgetData_competenciaSemFolha_retornaSemDados() throws Exception {
        WidgetDataDTO dto = WidgetDataDTO.semDados("kpi-total-funcionarios", "2024-01");
        when(dashboardWidgetQueryService.consultar(eq("gestor"), eq("kpi-total-funcionarios"), any()))
            .thenReturn(dto);

        mockMvc.perform(get("/dashboard/widgets/kpi-total-funcionarios/data")
                .param("competencia", "2024-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.semDados").value(true))
            .andExpect(jsonPath("$.competencia").value("2024-01"));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void getWidgetData_paramInvalido_retorna400() throws Exception {
        mockMvc.perform(get("/dashboard/widgets/kpi-total-funcionarios/data")
                .param("foo", "bar"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void getWidgetData_topNValido_retorna200() throws Exception {
        WidgetDataDTO dto = new WidgetDataDTO(
            "lista-top-proventos", "2024-06", false,
            null, null, null, null, null,
            null, null, null,
            java.util.List.of(new RubricaStatsDTO(1L, "001", "Salário", BigDecimal.TEN, 1L)),
            null, null);
        when(dashboardWidgetQueryService.consultar(eq("gestor"), eq("lista-top-proventos"), any()))
            .thenReturn(dto);

        mockMvc.perform(get("/dashboard/widgets/lista-top-proventos/data")
                .param("topN", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topProventos[0].codigo").value("001"));
    }
}
