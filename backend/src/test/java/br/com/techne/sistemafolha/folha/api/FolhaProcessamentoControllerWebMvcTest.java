package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.folha.application.FolhaProcessamentoService;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FolhaProcessamentoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FolhaProcessamentoControllerWebMvcTest {

    private static final String BODY_VALIDO = """
        {
          "competenciaInicio": "2026-01-01",
          "competenciaFim": "2026-01-31",
          "decimoTerceiro": false
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FolhaProcessamentoService folhaProcessamentoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void processar_semAuth_retorna403() throws Exception {
        mockMvc.perform(post("/folha-pagamento/processar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void processar_bodyValido_retorna200() throws Exception {
        when(folhaProcessamentoService.processar(
            eq(LocalDate.parse("2026-01-01")),
            eq(LocalDate.parse("2026-01-31")),
            eq(false),
            any(ProcessamentoOpcoes.class)))
            .thenReturn(new ProcessamentoResultadoDTO(10, 8, 2));

        mockMvc.perform(post("/folha-pagamento/processar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFichas").value(10));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void processar_competenciaOmitida_retorna400() throws Exception {
        mockMvc.perform(post("/folha-pagamento/processar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decimoTerceiro\": false}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }
}
