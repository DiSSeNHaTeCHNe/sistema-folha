package br.com.techne.sistemafolha.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String LOGIN = "usuario.teste";

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

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
    void doFilterInternal_semHeader_naoAutentica() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenInvalido_naoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer token-invalido");
        when(jwtService.extractLogin("token-invalido")).thenThrow(new RuntimeException("Token malformado"));

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenValido_configuraSecurityContext() throws Exception {
        String token = "token-valido";
        UserDetails userDetails = userDetails();
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractLogin(token)).thenReturn(LOGIN);
        when(userDetailsService.loadUserByUsername(LOGIN)).thenReturn(userDetails);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEqualsPrincipal(LOGIN);
        verify(filterChain).doFilter(request, response);
    }

    private UserDetails userDetails() {
        return User.builder()
            .username(LOGIN)
            .password("secret")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();
    }

    private void assertEqualsPrincipal(String expectedLogin) {
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(expectedLogin, SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
