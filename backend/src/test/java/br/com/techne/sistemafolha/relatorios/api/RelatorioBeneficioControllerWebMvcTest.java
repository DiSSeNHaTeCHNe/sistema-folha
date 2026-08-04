package br.com.techne.sistemafolha.relatorios.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.relatorios.application.RelatorioGeracaoService;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioIndisponivelException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelatorioBeneficioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RelatorioBeneficioControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RelatorioGeracaoService relatorioGeracaoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void gerar_semAuth_retorna403() throws Exception {
        mockMvc.perform(post("/relatorios/beneficio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":1,\"ano\":2024}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void gerar_competenciaFutura_retorna400() throws Exception {
        YearMonth futura = YearMonth.now().plusMonths(1);
        when(relatorioGeracaoService.gerarBeneficio(
            eq("gestor@teste.com"), eq(futura.getMonthValue()), eq(futura.getYear())))
            .thenThrow(new IllegalArgumentException("Competência futura não permitida"));

        mockMvc.perform(post("/relatorios/beneficio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":" + futura.getMonthValue() + ",\"ano\":" + futura.getYear() + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void gerar_valido_retorna200ComDto() throws Exception {
        YearMonth atual = YearMonth.now();
        RelatorioBeneficioDTO dto = new RelatorioBeneficioDTO(
            2L, atual.getMonthValue(), atual.getYear(),
            new BigDecimal("2000"), new BigDecimal("10000"),
            RelatorioStatus.PROCESSADO, null, null, null, false);
        when(relatorioGeracaoService.gerarBeneficio(eq("gestor@teste.com"), anyInt(), anyInt()))
            .thenReturn(dto);

        mockMvc.perform(post("/relatorios/beneficio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":" + atual.getMonthValue() + ",\"ano\":" + atual.getYear() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(2))
            .andExpect(jsonPath("$.status").value("PROCESSADO"));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void download_pendente_retorna409() throws Exception {
        when(relatorioGeracaoService.downloadPdf("gestor@teste.com", 2L, RelatorioTipo.BENEFICIO))
            .thenThrow(new RelatorioIndisponivelException(RelatorioStatus.PENDENTE));

        mockMvc.perform(get("/relatorios/beneficio/2/download"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void download_processado_retornaPdf() throws Exception {
        byte[] pdf = "%PDF-benef".getBytes(StandardCharsets.UTF_8);
        when(relatorioGeracaoService.downloadPdf("gestor@teste.com", 2L, RelatorioTipo.BENEFICIO))
            .thenReturn(pdf);

        mockMvc.perform(get("/relatorios/beneficio/2/download"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(pdf));
    }
}
