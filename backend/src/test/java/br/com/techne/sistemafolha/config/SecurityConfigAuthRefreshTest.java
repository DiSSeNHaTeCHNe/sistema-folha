package br.com.techne.sistemafolha.config;

import br.com.techne.sistemafolha.auth.api.AuthController;
import br.com.techne.sistemafolha.auth.api.RefreshTokenRequest;
import br.com.techne.sistemafolha.auth.api.TokenDTO;
import br.com.techne.sistemafolha.auth.application.AuthenticationService;
import br.com.techne.sistemafolha.auth.domain.RefreshTokenInvalidoException;
import br.com.techne.sistemafolha.exception.GlobalExceptionHandler;
import br.com.techne.sistemafolha.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SecurityConfigAuthRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void postAuthRefresh_anonimoSemAuthorization_naoRetorna401() throws Exception {
        TokenDTO tokenDTO = new TokenDTO(
                "user",
                "access-token",
                "new-refresh-token",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(7)
        );
        when(authenticationService.refreshToken(anyString())).thenReturn(tokenDTO);

        RefreshTokenRequest body = new RefreshTokenRequest("valid-refresh-token");

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(401);
    }

    @Test
    void postAuthRefresh_tokenInvalido_retorna401Nao500() throws Exception {
        when(authenticationService.refreshToken(anyString()))
            .thenThrow(new RefreshTokenInvalidoException("Refresh token inválido ou expirado"));

        RefreshTokenRequest body = new RefreshTokenRequest("invalid-refresh-token");

        MvcResult result = mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Refresh token inválido ou expirado"))
            .andReturn();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(500);
    }

    @Test
    void getAuthAcesso_anonimoSemAuthorization_exigeAutenticacao() throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/acesso"))
                .andExpect(status().is4xxClientError())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
    }
}