package br.com.techne.sistemafolha.security;

import jakarta.servlet.FilterChain;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
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
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        logAppender = capturarLogsWriteGuard();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (logAppender != null) {
            Logger logger = (Logger) LoggerFactory.getLogger(ApiKeyWriteGuardFilter.class);
            logger.detachAppender(logAppender);
        }
    }

    @Test
    void doFilterInternal_postComApiKeyReadOnly_emiteWarnSemSecret() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.POST.name());
        request.setRequestURI("/folha-pagamento/processar");
        request.addHeader("Authorization", "Bearer sf_live_abc12345secretvalue");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(logAppender.list.stream().anyMatch(e ->
                e.getLevel().toString().equals("WARN")
                        && e.getFormattedMessage().contains("admin")
                        && e.getFormattedMessage().contains("POST")
                        && e.getFormattedMessage().contains("/folha-pagamento/processar")));
        assertTrue(logAppender.list.stream().noneMatch(e ->
                e.getFormattedMessage().contains("sf_live_")
                        || e.getFormattedMessage().contains("Authorization")));
    }

    @Test
    void doFilterInternal_putComApiKeyReadOnly_retorna403() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.PUT.name());
        request.setRequestURI("/funcionarios/1");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_patchComApiKeyReadOnly_retorna403() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.PATCH.name());
        request.setRequestURI("/funcionarios/1");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
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

    @Test
    void doFilterInternal_postWorkspaceProposalsComApiKeyWorkspace_continuaChain() throws Exception {
        configurarAuthComMarkerWorkspace();
        request.setMethod(HttpMethod.POST.name());
        request.setRequestURI("/workspace/proposals");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_postWorkspaceDatasetsComApiKeyWorkspace_retorna403() throws Exception {
        configurarAuthComMarkerWorkspace();
        request.setMethod(HttpMethod.POST.name());
        request.setRequestURI("/workspace/datasets");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilterInternal_postWorkspaceProposalsComApiKeyReadOnly_retorna403() throws Exception {
        configurarAuthComMarkerReadOnly();
        request.setMethod(HttpMethod.POST.name());
        request.setRequestURI("/workspace/proposals");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void isWorkspaceProposalPath_reconhecePrefixo() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/workspace/proposals/1/confirmar");
        assertTrue(ApiKeyWriteGuardFilter.isWorkspaceProposalPath(req));
    }

    @Test
    void isApiKeyWorkspaceAuth_semAutenticacao_retornaFalse() {
        assertFalse(ApiKeyWriteGuardFilter.isApiKeyWorkspaceAuth());
    }

    private void configurarAuthComMarkerWorkspace() {
        var user = User.builder()
                .username("agent")
                .password("secret")
                .authorities(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority(ApiKeySecurity.ROLE_API_KEY_WORKSPACE))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
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

    private ListAppender<ILoggingEvent> capturarLogsWriteGuard() {
        Logger logger = (Logger) LoggerFactory.getLogger(ApiKeyWriteGuardFilter.class);
        logger.setLevel(ch.qos.logback.classic.Level.WARN);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
