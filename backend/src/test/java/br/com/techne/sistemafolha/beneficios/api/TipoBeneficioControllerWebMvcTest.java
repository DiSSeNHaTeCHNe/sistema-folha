package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.beneficios.application.TipoBeneficioService;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioCodigoDuplicadoException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.config.SecurityConfig;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TipoBeneficioController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class TipoBeneficioControllerWebMvcTest {

    private static final String BODY_VALIDO = """
        {"codigo":"VR","descricao":"Vale Refeição","ativo":true}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TipoBeneficioService tipoBeneficioService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    void listarAtivos_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/tipo-beneficio"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listarAtivos_retorna200() throws Exception {
        when(tipoBeneficioService.listarAtivos()).thenReturn(List.of());

        mockMvc.perform(get("/tipo-beneficio"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void criar_sucesso_retorna200() throws Exception {
        when(tipoBeneficioService.criar(any())).thenReturn(
            new TipoBeneficioDTO(1L, "VR", "Vale Refeição", true));

        mockMvc.perform(post("/tipo-beneficio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void criar_codigoVazio_retorna400() throws Exception {
        mockMvc.perform(post("/tipo-beneficio")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\":\"\",\"descricao\":\"Teste\",\"ativo\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void atualizar_sucesso_retorna200() throws Exception {
        when(tipoBeneficioService.atualizar(eq(1L), any()))
            .thenReturn(new TipoBeneficioDTO(1L, "VR", "Vale Refeição Atualizado", true));

        mockMvc.perform(put("/tipo-beneficio/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY_VALIDO))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void remover_sucesso_retorna204() throws Exception {
        mockMvc.perform(delete("/tipo-beneficio/1"))
            .andExpect(status().isNoContent());

        verify(tipoBeneficioService).remover(1L);
    }
}
