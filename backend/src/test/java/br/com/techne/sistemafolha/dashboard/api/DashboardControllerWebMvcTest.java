package br.com.techne.sistemafolha.dashboard.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.dashboard.application.DashboardService;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DashboardControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void getStats_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/dashboard/stats"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void getStats_retorna200() throws Exception {
        DashboardStatsDTO stats = new DashboardStatsDTO(
            10L, java.math.BigDecimal.TEN, 5L,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
            java.math.BigDecimal.ONE, java.math.BigDecimal.ONE,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        when(dashboardService.getStats("user")).thenReturn(stats);

        mockMvc.perform(get("/dashboard/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFuncionarios").value(10));
    }
}
