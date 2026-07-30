package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.domain.Usuario;
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
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ResumoFolhaPagamentoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ApiKeyAclWebMvcTest {

    private static final String API_KEY_BEARER = "Bearer sf_live_testkey1234567890abcdefghij";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResumoFolhaPagamentoService resumoFolhaPagamentoService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void getResumoFolhaPagamento_jwtComListaVazia_retorna200() throws Exception {
        when(resumoFolhaPagamentoService.listarTodos(eq("gestor"), eq(2024), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/resumo-folha-pagamento").param("ano", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getResumoFolhaPagamento_bearerApiKeyComListaVazia_retorna200() throws Exception {
        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));
        when(resumoFolhaPagamentoService.listarTodos(eq("gestor"), eq(2024), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/resumo-folha-pagamento")
                        .param("ano", "2024")
                        .header("Authorization", API_KEY_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "gestor", roles = "USER")
    void getResumoFolhaPagamento_jwtEBearerApiKey_retornamMesmosDados() throws Exception {
        ResumoFolhaPagamentoDTO resumo = resumoExemplo();
        when(resumoFolhaPagamentoService.listarTodos(eq("gestor"), eq(2024), isNull()))
                .thenReturn(List.of(resumo));

        mockMvc.perform(get("/resumo-folha-pagamento").param("ano", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].totalBruto").value(10000));

        Usuario gestor = usuarioGestor();
        when(apiKeyService.autenticarPorChave(startsWith("sf_live_"))).thenReturn(Optional.of(gestor));

        mockMvc.perform(get("/resumo-folha-pagamento")
                        .param("ano", "2024")
                        .header("Authorization", API_KEY_BEARER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].totalBruto").value(10000));
    }

    private Usuario usuarioGestor() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setLogin("gestor");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("GESTOR", "API_KEY"));
        return usuario;
    }

    private ResumoFolhaPagamentoDTO resumoExemplo() {
        return new ResumoFolhaPagamentoDTO(
                1L,
                10,
                new BigDecimal("2000.00"),
                new BigDecimal("8000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("7000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("12000.00"),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31),
                LocalDateTime.of(2024, 2, 1, 10, 0),
                false,
                true
        );
    }
}
