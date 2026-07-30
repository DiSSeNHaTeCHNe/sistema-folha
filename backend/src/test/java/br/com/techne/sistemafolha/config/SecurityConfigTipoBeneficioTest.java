package br.com.techne.sistemafolha.config;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.beneficios.api.TipoBeneficioController;
import br.com.techne.sistemafolha.beneficios.api.TipoBeneficioDTO;
import br.com.techne.sistemafolha.beneficios.application.TipoBeneficioService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TipoBeneficioController.class)
@Import(SecurityConfig.class)
class SecurityConfigTipoBeneficioTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TipoBeneficioService tipoBeneficioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    @WithMockUser(roles = "USER")
    void postTipoBeneficio_semAdmin_retorna403() throws Exception {
        TipoBeneficioDTO body = new TipoBeneficioDTO(null, "VALE_REFEICAO", "Vale Refeição", true);

        mockMvc.perform(post("/tipo-beneficio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ACESSO_TOTAL")
    void postTipoBeneficio_comAcessoTotal_retorna403() throws Exception {
        TipoBeneficioDTO body = new TipoBeneficioDTO(null, "VALE_REFEICAO", "Vale Refeição", true);

        mockMvc.perform(post("/tipo-beneficio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void postTipoBeneficio_comAdmin_retorna2xx() throws Exception {
        TipoBeneficioDTO request = new TipoBeneficioDTO(null, "SEGUROS", "Seguros - Custo Empresa", true);
        TipoBeneficioDTO response = new TipoBeneficioDTO(1L, "SEGUROS", "Seguros - Custo Empresa", true);
        when(tipoBeneficioService.criar(any(TipoBeneficioDTO.class))).thenReturn(response);

        mockMvc.perform(post("/tipo-beneficio")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());
    }
}
