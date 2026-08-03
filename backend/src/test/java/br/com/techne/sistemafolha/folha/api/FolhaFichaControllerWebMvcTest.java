package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.folha.application.FolhaFichaConsultaService;
import br.com.techne.sistemafolha.folha.domain.FichaMensalNotFoundException;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FolhaFichaController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class FolhaFichaControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FolhaFichaConsultaService folhaFichaConsultaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void buscarFichaPorFuncionario_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/folha-pagamento/fichas/por-funcionario")
                .param("funcionarioId", "1")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void buscarFichaPorFuncionario_encontrada_retorna200() throws Exception {
        when(folhaFichaConsultaService.buscarFichaIdPorFuncionario(
            eq("user"), eq(1L), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31")), eq(false)))
            .thenReturn(42L);

        mockMvc.perform(get("/folha-pagamento/fichas/por-funcionario")
                .param("funcionarioId", "1")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void buscarFichaPorFuncionario_decimoTerceiroTrue() throws Exception {
        when(folhaFichaConsultaService.buscarFichaIdPorFuncionario(
            eq("user"), eq(1L), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31")), eq(true)))
            .thenReturn(43L);

        mockMvc.perform(get("/folha-pagamento/fichas/por-funcionario")
                .param("funcionarioId", "1")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31")
                .param("decimoTerceiro", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(43));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void buscarFichaPorFuncionario_naoEncontrada_retorna404() throws Exception {
        when(folhaFichaConsultaService.buscarFichaIdPorFuncionario(
            eq("user"), eq(99L), eq(java.time.LocalDate.parse("2026-01-01")),
            eq(java.time.LocalDate.parse("2026-01-31")), eq(false)))
            .thenThrow(new FichaMensalNotFoundException(99L));

        mockMvc.perform(get("/folha-pagamento/fichas/por-funcionario")
                .param("funcionarioId", "99")
                .param("dataInicio", "2026-01-01")
                .param("dataFim", "2026-01-31"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void listarLinhasPorTotalizador_retorna200() throws Exception {
        when(folhaFichaConsultaService.listarLinhasPorTotalizador(
            eq("user"), eq(1L), eq(Totalizador.GROSS)))
            .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/folha-pagamento/fichas/1/linhas")
                .param("totalizer", "GROSS"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void listarLinhasPorTotalizador_fichaInexistente_retorna404() throws Exception {
        when(folhaFichaConsultaService.listarLinhasPorTotalizador(
            eq("user"), eq(99L), eq(Totalizador.NET)))
            .thenThrow(new FichaMensalNotFoundException(99L));

        mockMvc.perform(get("/folha-pagamento/fichas/99/linhas")
                .param("totalizer", "NET"))
            .andExpect(status().isNotFound());
    }
}
