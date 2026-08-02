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
 * Bloqueia mutações HTTP quando a autenticação foi feita via API Key (marker read-only).
 */
@Component
public class ApiKeyWriteGuardFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyWriteGuardFilter.class);

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
        if (isMutatingMethod(request.getMethod()) && isApiKeyReadOnlyAuth()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.warn("API Key write blocked login={} method={} uri={}",
                    authentication.getName(), request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
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
}
