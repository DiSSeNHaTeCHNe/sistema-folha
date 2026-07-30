package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.beneficios.application.BeneficioMensalService;
import br.com.techne.sistemafolha.config.SecurityConfig;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BeneficioMensalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class BeneficioMensalControllerWebMvcTest {

    private static final String API_KEY_BEARER = "Bearer sf_live_testkey1234567890abcdefghij";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BeneficioMensalService beneficioMensalService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

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

    @Test
    @WithMockUser(username = "usuario.teste", roles = "USER")
    void listarCompetencias_delegaAoService() throws Exception {
        when(beneficioMensalService.listarCompetenciasParaUsuario(
            eq("usuario.teste"), eq(2026), eq(3)))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/beneficio-mensal/competencias")
                .param("ano", "2026")
                .param("mes", "3"))
            .andExpect(status().isOk());

        verify(beneficioMensalService).listarCompetenciasParaUsuario(
            eq("usuario.teste"), eq(2026), eq(3));
    }

    @Test
    @WithMockUser(username = "usuario.teste", roles = "USER")
    void listarPorCompetencia_semParams_retorna400() throws Exception {
        mockMvc.perform(get("/beneficio-mensal"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("competenciaInicio: obrigatório"));
    }

    @Test
    void listarPorCompetencia_bearerApiKey_semParams_retorna400() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("usuario.teste");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("API_KEY"));
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(usuario));

        mockMvc.perform(get("/beneficio-mensal").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("competenciaInicio: obrigatório"));
    }
}
