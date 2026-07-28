package br.com.techne.sistemafolha.config;

import br.com.techne.sistemafolha.folha.api.FolhaProcessamentoController;
import br.com.techne.sistemafolha.folha.api.ProcessamentoRequestDTO;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.application.FolhaProcessamentoService;
import br.com.techne.sistemafolha.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FolhaProcessamentoController.class)
@Import(SecurityConfig.class)
class SecurityConfigFolhaProcessamentoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FolhaProcessamentoService folhaProcessamentoService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void postProcessar_semAdmin_retorna403() throws Exception {
        ProcessamentoRequestDTO body = new ProcessamentoRequestDTO(
            LocalDate.of(2024, 10, 1),
            LocalDate.of(2024, 10, 31),
            false,
            null);

        mockMvc.perform(post("/folha-pagamento/processar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ACESSO_TOTAL")
    void postProcessar_comAcessoTotal_retorna403() throws Exception {
        ProcessamentoRequestDTO body = new ProcessamentoRequestDTO(
            LocalDate.of(2024, 10, 1),
            LocalDate.of(2024, 10, 31),
            false,
            null);

        mockMvc.perform(post("/folha-pagamento/processar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postProcessar_comAdmin_retorna2xx() throws Exception {
        ProcessamentoRequestDTO request = new ProcessamentoRequestDTO(
            LocalDate.of(2024, 10, 1),
            LocalDate.of(2024, 10, 31),
            false,
            null);
        when(folhaProcessamentoService.processar(any(), any(), anyBoolean(), any()))
            .thenReturn(new ProcessamentoResultadoDTO(1, 2, 1));

        mockMvc.perform(post("/folha-pagamento/processar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());
    }
}
