package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.beneficios.application.ImportacaoBeneficioMensalService;
import br.com.techne.sistemafolha.beneficios.domain.ImportacaoBeneficioMensalInvalidaException;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportacaoBeneficioMensalController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class ImportacaoBeneficioMensalControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportacaoBeneficioMensalService importacaoBeneficioMensalService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void importarBeneficiosMensais_semAuth_retorna403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "beneficios.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});

        mockMvc.perform(multipart("/importacao/beneficios-mensais")
                .file(file)
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importarBeneficiosMensais_sucesso_retorna200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "beneficios.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        ImportacaoResultadoDTO resultado = new ImportacaoResultadoDTO(5, 0, BigDecimal.TEN, List.of());
        when(importacaoBeneficioMensalService.importar(any(), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31")), eq(false)))
            .thenReturn(resultado);

        mockMvc.perform(multipart("/importacao/beneficios-mensais")
                .file(file)
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processadas").value(5));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importarBeneficiosMensais_rejeitada_retorna400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "beneficios.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        when(importacaoBeneficioMensalService.importar(any(), any(), any(), eq(false)))
            .thenThrow(new ImportacaoBeneficioMensalInvalidaException(List.of("Arquivo inválido")));

        mockMvc.perform(multipart("/importacao/beneficios-mensais")
                .file(file)
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importarBeneficiosMensais_ioException_retorna400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "beneficios.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        when(importacaoBeneficioMensalService.importar(any(), any(), any(), eq(false)))
            .thenThrow(new IOException("corrupted"));

        mockMvc.perform(multipart("/importacao/beneficios-mensais")
                .file(file)
                .param("competenciaInicio", "2026-01-01")
                .param("competenciaFim", "2026-01-31"))
            .andExpect(status().isBadRequest());
    }
}
