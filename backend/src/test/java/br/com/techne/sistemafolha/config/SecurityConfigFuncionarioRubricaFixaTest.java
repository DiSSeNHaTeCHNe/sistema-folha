package br.com.techne.sistemafolha.config;

import br.com.techne.sistemafolha.cadastros.api.FuncionarioRubricaFixaController;
import br.com.techne.sistemafolha.cadastros.api.FuncionarioRubricaFixaDTO;
import br.com.techne.sistemafolha.cadastros.application.FuncionarioRubricaFixaService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FuncionarioRubricaFixaController.class)
@Import(SecurityConfig.class)
class SecurityConfigFuncionarioRubricaFixaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FuncionarioRubricaFixaService funcionarioRubricaFixaService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "USER")
    void getRubricasFixas_semAdmin_retorna403() throws Exception {
        mockMvc.perform(get("/funcionario-rubrica-fixa"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void postRubricaFixa_semAdmin_retorna403() throws Exception {
        FuncionarioRubricaFixaDTO body = new FuncionarioRubricaFixaDTO(
            null, 1L, 2L, new BigDecimal("500.00"),
            LocalDate.of(2024, 10, 1), null, null, true, null, null, null, null);

        mockMvc.perform(post("/funcionario-rubrica-fixa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRubricasFixas_comAdmin_retorna2xx() throws Exception {
        when(funcionarioRubricaFixaService.listar(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/funcionario-rubrica-fixa"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postRubricaFixa_comAdmin_retorna2xx() throws Exception {
        FuncionarioRubricaFixaDTO request = new FuncionarioRubricaFixaDTO(
            null, 1L, 2L, new BigDecimal("500.00"),
            LocalDate.of(2024, 10, 1), null, null, true, null, null, null, null);
        FuncionarioRubricaFixaDTO response = new FuncionarioRubricaFixaDTO(
            10L, 1L, 2L, new BigDecimal("500.00"),
            LocalDate.of(2024, 10, 1), null, null, true, "João", "900", "Ajuda", null);
        when(funcionarioRubricaFixaService.criar(any(FuncionarioRubricaFixaDTO.class))).thenReturn(response);

        mockMvc.perform(post("/funcionario-rubrica-fixa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());
    }
}
