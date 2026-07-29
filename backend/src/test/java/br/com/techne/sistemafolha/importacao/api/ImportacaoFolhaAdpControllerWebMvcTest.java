package br.com.techne.sistemafolha.importacao.api;

import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.folha.domain.FolhaProcessamentoFalhaException;
import br.com.techne.sistemafolha.importacao.application.ImportacaoFolhaAdpService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportacaoFolhaAdpController.class)
@Import(SecurityConfig.class)
class ImportacaoFolhaAdpControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportacaoFolhaAdpService importacaoFolhaAdpService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importarFolhaAdp_filenameNull_retorna400() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", null, "text/plain", "conteudo".getBytes());

        mockMvc.perform(multipart("/importacao/folha-adp")
                .file(arquivo)
                .param("decimoTerceiro", "false")
                .param("confirmarSubstituicao", "false"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Nome do arquivo não informado"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importarFolhaAdp_processamentoFalha_retorna500ComPrefixo() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "folha.txt", "text/plain", "conteudo".getBytes());

        when(importacaoFolhaAdpService.importarFolhaAdp(any(), eq(false), eq(false)))
            .thenThrow(new FolhaProcessamentoFalhaException("Falha interna do motor"));

        mockMvc.perform(multipart("/importacao/folha-adp")
                .file(arquivo)
                .param("decimoTerceiro", "false")
                .param("confirmarSubstituicao", "false"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message")
                .value("Falha no processamento da ficha: Falha interna do motor"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importarFolhaAdp_erroImportacao_retorna400() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "folha.txt", "text/plain", "conteudo".getBytes());

        when(importacaoFolhaAdpService.importarFolhaAdp(any(), eq(false), eq(false)))
            .thenThrow(new RuntimeException("Funcionários não encontrados"));

        mockMvc.perform(multipart("/importacao/folha-adp")
                .file(arquivo)
                .param("decimoTerceiro", "false")
                .param("confirmarSubstituicao", "false"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message")
                .value("Erro ao importar arquivo ADP: Funcionários não encontrados"));
    }
}
