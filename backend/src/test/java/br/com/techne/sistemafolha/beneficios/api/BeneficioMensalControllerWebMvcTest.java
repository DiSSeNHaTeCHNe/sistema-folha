package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.beneficios.application.BeneficioMensalService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BeneficioMensalController.class)
@Import(SecurityConfig.class)
class BeneficioMensalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BeneficioMensalService beneficioMensalService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "usuario.teste", roles = "USER")
    void listarPorCompetencia_delegaAoService() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fim = LocalDate.of(2026, 1, 31);
        when(beneficioMensalService.listarPorCompetenciaParaUsuario(
            eq("usuario.teste"), eq(inicio), eq(fim)))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/beneficio-mensal")
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isOk());

        verify(beneficioMensalService).listarPorCompetenciaParaUsuario(
            eq("usuario.teste"), eq(inicio), eq(fim));
    }
}
