package br.com.techne.sistemafolha.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Bloqueia mutações HTTP quando a autenticação foi feita via API Key read-only.
 * API Keys com {@link ApiKeySecurity#ROLE_API_KEY_WORKSPACE} podem mutar somente
 * em {@code /workspace/proposals/**} (WKS-25).
 */
@Component
public class ApiKeyWriteGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyWriteGuardFilter.class);

    static final String WORKSPACE_PROPOSAL_PREFIX = "/workspace/proposals";

    private static final Set<String> MUTATING_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name()
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isMutatingMethod(request.getMethod())) {
            if (isApiKeyReadOnlyAuth()) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                log.warn("API Key write blocked login={} method={} uri={}",
                        authentication.getName(), request.getMethod(), request.getRequestURI());
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (isApiKeyWorkspaceAuth() && !isWorkspaceProposalPath(request)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                log.warn("API Key workspace write blocked outside proposals login={} method={} uri={}",
                        authentication.getName(), request.getMethod(), request.getRequestURI());
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    static boolean isMutatingMethod(String method) {
        return method != null && MUTATING_METHODS.contains(method);
    }

    static boolean isApiKeyReadOnlyAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> ApiKeySecurity.ROLE_API_KEY_READONLY.equals(a.getAuthority()));
    }

    static boolean isApiKeyWorkspaceAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> ApiKeySecurity.ROLE_API_KEY_WORKSPACE.equals(a.getAuthority()));
    }

    static boolean isWorkspaceProposalPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return false;
        }
        String normalized = uri.startsWith("/api") ? uri.substring(4) : uri;
        return normalized.startsWith(WORKSPACE_PROPOSAL_PREFIX);
    }
}
