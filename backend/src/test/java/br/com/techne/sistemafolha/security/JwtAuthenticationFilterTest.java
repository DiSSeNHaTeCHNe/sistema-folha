package br.com.techne.sistemafolha.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void doFilterInternal_headerMalformado_naoLogaValorDoHeader() throws Exception {
        String tokenLiteral = "Bearer eyJhbGciOiJIUzI1NiJ9.token";
        request.addHeader("Authorization", tokenLiteral.substring(7));

        ListAppender<ILoggingEvent> appender = capturarLogsJwtFilter();

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        assertTrue(appender.list.stream()
            .noneMatch(e -> e.getFormattedMessage().contains("eyJhbGciOiJIUzI1NiJ9")));
        assertTrue(appender.list.stream()
            .anyMatch(e -> e.getFormattedMessage().contains("Bearer esperado")));
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

    private ListAppender<ILoggingEvent> capturarLogsJwtFilter() {
        Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
