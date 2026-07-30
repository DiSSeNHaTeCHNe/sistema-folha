package br.com.techne.sistemafolha.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApiKeyWriteGuardFilterTest {

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ApiKeyWriteGuardFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_postComApiKeyReadOnly_retorna403() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.POST.name());
        request.setRequestURI("/folha-pagamento/processar");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_getComApiKeyReadOnly_continuaChain() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.GET.name());
        request.setRequestURI("/funcionarios");

        filter.doFilterInternal(request, response, filterChain);

        assertFalse(response.isCommitted());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_postComJwtSemMarker_naoBloqueia() throws Exception {
        configurarAuthJwt();
        request.setMethod(HttpMethod.POST.name());
        request.setRequestURI("/auth/api-keys");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void doFilterInternal_deleteAuthApiKeysComApiKey_retorna403() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.DELETE.name());
        request.setRequestURI("/auth/api-keys/42");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void isApiKeyReadOnlyAuth_semAutenticacao_retornaFalse() {
        assertFalse(ApiKeyWriteGuardFilter.isApiKeyReadOnlyAuth());
    }

    @Test
    void isMutatingMethod_identificaMetodosMutaveis() {
        assertTrue(ApiKeyWriteGuardFilter.isMutatingMethod("POST"));
        assertTrue(ApiKeyWriteGuardFilter.isMutatingMethod("DELETE"));
        assertFalse(ApiKeyWriteGuardFilter.isMutatingMethod("GET"));
    }

    private void configurarAuthComMarkerReadOnly() {
        var user = User.builder()
                .username("admin")
                .password("secret")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority(ApiKeySecurity.ROLE_API_KEY_READONLY))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private void configurarAuthJwt() {
        var user = User.builder()
                .username("admin")
                .password("secret")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
