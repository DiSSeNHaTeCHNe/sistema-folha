package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.auth.application.AuthenticationService;
import br.com.techne.sistemafolha.auth.domain.RefreshTokenInvalidoException;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private ApiKeyService apiKeyService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void login_credenciaisValidas_retorna200() throws Exception {
        TokenDTO token = new TokenDTO("user", "jwt-token", "refresh", LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusDays(7));
        when(authenticationService.authenticate(any(LoginDTO.class))).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"user\",\"senha\":\"secret\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_credenciaisInvalidas_retorna500() throws Exception {
        when(authenticationService.authenticate(any(LoginDTO.class)))
            .thenThrow(new org.springframework.security.core.userdetails.UsernameNotFoundException("Login inválido"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"user\",\"senha\":\"wrong\"}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void refresh_tokenValido_retorna200() throws Exception {
        TokenDTO token = new TokenDTO("user", "new-jwt", "new-refresh", LocalDateTime.now().plusHours(1),
            LocalDateTime.now().plusDays(7));
        when(authenticationService.refreshToken("valid-refresh")).thenReturn(token);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"valid-refresh\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("new-jwt"));
    }

    @Test
    void refresh_tokenExpirado_retorna401() throws Exception {
        when(authenticationService.refreshToken("expired"))
            .thenThrow(new RefreshTokenInvalidoException("Refresh token inválido"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"expired\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void logout_revogaToken_retorna200() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"token-to-revoke\"}"))
            .andExpect(status().isOk());

        verify(authenticationService).logout("token-to-revoke");
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void obterInformacoesAcesso_retorna200() throws Exception {
        AcessoUsuarioDTO acesso = AcessoUsuarioDTO.builder()
            .acessoTotal(true)
            .centrosCustoIds(Collections.emptySet())
            .build();
        when(authenticationService.obterAcessoUsuarioPorLogin("user")).thenReturn(acesso);

        mockMvc.perform(get("/auth/acesso"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.acessoTotal").value(true));
    }

    @Test
    void obterInformacoesAcesso_semAuth_retorna403() throws Exception {
        mockMvc.perform(get("/auth/acesso"))
            .andExpect(status().isForbidden());
    }
}
