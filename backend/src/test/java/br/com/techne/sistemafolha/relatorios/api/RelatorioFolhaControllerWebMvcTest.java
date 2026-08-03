package br.com.techne.sistemafolha.relatorios.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.relatorios.application.RelatorioGeracaoService;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioAcessoNegadoException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioIndisponivelException;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioNotFoundException;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RelatorioFolhaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RelatorioFolhaControllerWebMvcTest {

    private static final String API_KEY_BEARER = "Bearer sf_live_testkey1234567890abcdefghij";

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
        mockMvc.perform(post("/relatorios/folha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":1,\"ano\":2024}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void gerar_competenciaFutura_retorna400() throws Exception {
        YearMonth futura = YearMonth.now().plusMonths(1);
        when(relatorioGeracaoService.gerarFolha(
            eq("gestor@teste.com"), eq(futura.getMonthValue()), eq(futura.getYear())))
            .thenThrow(new IllegalArgumentException("Competência futura não permitida"));

        mockMvc.perform(post("/relatorios/folha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":" + futura.getMonthValue() + ",\"ano\":" + futura.getYear() + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void gerar_valido_retorna200ComDto() throws Exception {
        YearMonth atual = YearMonth.now();
        RelatorioFolhaDTO dto = new RelatorioFolhaDTO(
            1L, atual.getMonthValue(), atual.getYear(),
            100, new BigDecimal("5000"), new BigDecimal("500"),
            RelatorioStatus.PROCESSADO, null, null);
        when(relatorioGeracaoService.gerarFolha(eq("gestor@teste.com"), anyInt(), anyInt()))
            .thenReturn(dto);

        mockMvc.perform(post("/relatorios/folha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":" + atual.getMonthValue() + ",\"ano\":" + atual.getYear() + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PROCESSADO"));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void download_pendente_retorna409() throws Exception {
        when(relatorioGeracaoService.downloadPdf("gestor@teste.com", 1L, RelatorioTipo.FOLHA))
            .thenThrow(new RelatorioIndisponivelException(RelatorioStatus.PENDENTE));

        mockMvc.perform(get("/relatorios/folha/1/download"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void download_processado_retornaPdf() throws Exception {
        byte[] pdf = "%PDF-folha".getBytes(StandardCharsets.UTF_8);
        when(relatorioGeracaoService.downloadPdf("gestor@teste.com", 1L, RelatorioTipo.FOLHA))
            .thenReturn(pdf);

        mockMvc.perform(get("/relatorios/folha/1/download"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(pdf));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void download_erro_retorna409() throws Exception {
        when(relatorioGeracaoService.downloadPdf("gestor@teste.com", 1L, RelatorioTipo.FOLHA))
            .thenThrow(new RelatorioIndisponivelException(RelatorioStatus.ERRO));

        mockMvc.perform(get("/relatorios/folha/1/download"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void download_naoEncontrado_retorna404() throws Exception {
        when(relatorioGeracaoService.downloadPdf("gestor@teste.com", 99L, RelatorioTipo.FOLHA))
            .thenThrow(new RelatorioNotFoundException(99L));

        mockMvc.perform(get("/relatorios/folha/99/download"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void gerar_mesInvalido_retorna400BeanValidation() throws Exception {
        mockMvc.perform(post("/relatorios/folha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":13,\"ano\":2024}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void gerar_aclNegado_retorna403() throws Exception {
        YearMonth atual = YearMonth.now();
        when(relatorioGeracaoService.gerarFolha(
            eq("gestor@teste.com"), eq(atual.getMonthValue()), eq(atual.getYear())))
            .thenThrow(new RelatorioAcessoNegadoException());

        mockMvc.perform(post("/relatorios/folha")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":" + atual.getMonthValue() + ",\"ano\":" + atual.getYear() + "}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(username = "gestor@teste.com", roles = "USER")
    void listar_retornaRelatoriosOrdenadosAnoMesDesc() throws Exception {
        RelatorioFolhaDTO recente = new RelatorioFolhaDTO(
            2L, 6, 2026, 10, new BigDecimal("5000"), new BigDecimal("500"),
            RelatorioStatus.PROCESSADO, LocalDateTime.now(), null);
        RelatorioFolhaDTO antigo = new RelatorioFolhaDTO(
            1L, 1, 2024, 5, new BigDecimal("3000"), new BigDecimal("300"),
            RelatorioStatus.PROCESSADO, LocalDateTime.now(), null);
        when(relatorioGeracaoService.listarFolha("gestor@teste.com"))
            .thenReturn(List.of(recente, antigo));

        mockMvc.perform(get("/relatorios/folha"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ano").value(2026))
            .andExpect(jsonPath("$[0].mes").value(6))
            .andExpect(jsonPath("$[1].ano").value(2024))
            .andExpect(jsonPath("$[1].mes").value(1));
    }

    @Test
    void listar_bearerApiKey_retorna200() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("gestor@teste.com");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("API_KEY"));
        when(apiKeyService.autenticarPorChave(org.mockito.ArgumentMatchers.startsWith("sf_live_")))
            .thenReturn(Optional.of(usuario));
        when(relatorioGeracaoService.listarFolha("gestor@teste.com")).thenReturn(List.of());

        mockMvc.perform(get("/relatorios/folha").header("Authorization", API_KEY_BEARER))
            .andExpect(status().isOk());
    }

    @Test
    void gerar_bearerApiKey_retorna403() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("gestor@teste.com");
        usuario.setAtivo(true);
        usuario.setPermissoes(List.of("API_KEY"));
        when(apiKeyService.autenticarPorChave(org.mockito.ArgumentMatchers.startsWith("sf_live_")))
            .thenReturn(Optional.of(usuario));

        mockMvc.perform(post("/relatorios/folha")
                .header("Authorization", API_KEY_BEARER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mes\":1,\"ano\":2024}"))
            .andExpect(status().isForbidden());
    }
}
