package br.com.techne.sistemafolha.dashboard.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.dashboard.application.DashboardLayoutService;
import br.com.techne.sistemafolha.dashboard.application.DashboardWidgetCatalogService;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardLayoutController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardLayoutControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardLayoutService dashboardLayoutService;

    @MockBean
    private DashboardWidgetCatalogService dashboardWidgetCatalogService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void getLayout_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/dashboard/layout"))
            .andExpect(status().isForbidden());
    }

    @Test
    void getCatalog_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/dashboard/widgets/catalog"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void getLayout_semEscopo_retorna403() throws Exception {
        when(dashboardLayoutService.obterOuCriarPadrao("user-a"))
            .thenThrow(new DashboardAcessoNegadoException());

        mockMvc.perform(get("/dashboard/layout"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void getLayout_comEscopo_retorna200() throws Exception {
        DashboardLayoutDTO layout = new DashboardLayoutDTO(1L, "Meu dashboard", List.of(
            new WidgetInstanceDTO("kpi-total-funcionarios", "abc12345", 0, 3, 1, null)));
        when(dashboardLayoutService.obterOuCriarPadrao("user-a")).thenReturn(layout);

        mockMvc.perform(get("/dashboard/layout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.widgets[0].widgetId").value("kpi-total-funcionarios"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void getCatalog_comEscopo_retorna200() throws Exception {
        when(dashboardWidgetCatalogService.listarParaUsuario("user-a")).thenReturn(List.of(
            new WidgetCatalogItemDTO("kpi-total-funcionarios", "Total", "Desc", "KPI", 3, 1)));

        mockMvc.perform(get("/dashboard/widgets/catalog"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].widgetId").value("kpi-total-funcionarios"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void putLayout_payloadInvalido_retorna400() throws Exception {
        mockMvc.perform(put("/dashboard/layout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"x\",\"widgets\":[{\"widgetId\":\"\",\"instanceId\":\"a\",\"ordem\":0,\"colSpan\":3,\"rowSpan\":1}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void putLayout_colSpanInvalido_retorna400() throws Exception {
        mockMvc.perform(put("/dashboard/layout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"x\",\"widgets\":[{\"widgetId\":\"kpi-total-funcionarios\",\"instanceId\":\"a\",\"ordem\":0,\"colSpan\":99,\"rowSpan\":1}]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void putLayout_valido_retorna200() throws Exception {
        DashboardLayoutDTO salvo = new DashboardLayoutDTO(1L, "Custom", List.of(
            new WidgetInstanceDTO("kpi-total-funcionarios", "a", 0, 3, 1, null)));
        when(dashboardLayoutService.salvar(eq("user-a"), any(DashboardLayoutDTO.class))).thenReturn(salvo);

        mockMvc.perform(put("/dashboard/layout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Custom\",\"widgets\":[{\"widgetId\":\"kpi-total-funcionarios\",\"instanceId\":\"a\",\"ordem\":0,\"colSpan\":3,\"rowSpan\":1}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Custom"));
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void deleteLayout_comEscopo_retorna204() throws Exception {
        mockMvc.perform(delete("/dashboard/layout"))
            .andExpect(status().isNoContent());

        verify(dashboardLayoutService).restaurarPadrao("user-a");
    }

    @Test
    @WithMockUser(username = "user-b", roles = "USER")
    void getLayout_usuarioB_usaLoginCorreto() throws Exception {
        DashboardLayoutDTO layoutB = new DashboardLayoutDTO(2L, "Layout B", List.of());
        when(dashboardLayoutService.obterOuCriarPadrao("user-b")).thenReturn(layoutB);

        mockMvc.perform(get("/dashboard/layout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Layout B"));

        verify(dashboardLayoutService).obterOuCriarPadrao("user-b");
    }

    @Test
    @WithMockUser(username = "user-a", roles = "USER")
    void getLayout_isolamentoUsuarios_naoChamaLoginDeOutroUsuario() throws Exception {
        DashboardLayoutDTO layoutA = new DashboardLayoutDTO(1L, "Layout A", List.of());
        when(dashboardLayoutService.obterOuCriarPadrao("user-a")).thenReturn(layoutA);

        mockMvc.perform(get("/dashboard/layout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Layout A"));

        verify(dashboardLayoutService).obterOuCriarPadrao("user-a");
    }
}
