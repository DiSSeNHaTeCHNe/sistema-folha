package br.com.techne.sistemafolha.security;



import br.com.techne.sistemafolha.auth.application.ApiKeyService;

import br.com.techne.sistemafolha.auth.domain.Usuario;

import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;



import java.io.IOException;

import java.util.ArrayList;

import java.util.List;



@Component

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);



    private final JwtService jwtService;

    private final UserDetailsService userDetailsService;

    private final ApiKeyService apiKeyService;



    public JwtAuthenticationFilter(

            JwtService jwtService,

            UserDetailsService userDetailsService,

            ApiKeyService apiKeyService) {

        this.jwtService = jwtService;

        this.userDetailsService = userDetailsService;

        this.apiKeyService = apiKeyService;

    }



    @Override

    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain

    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");



        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            log.debug("Header de autorização ausente ou malformado (Bearer esperado)");

            filterChain.doFilter(request, response);

            return;

        }



        final String bearerToken = authHeader.substring(7);



        if (bearerToken.startsWith(ApiKeySecurity.CHAVE_PREFIX)) {

            autenticarApiKey(request, bearerToken);

            filterChain.doFilter(request, response);

            return;

        }



        autenticarJwt(request, bearerToken);

        filterChain.doFilter(request, response);

    }



    private void autenticarApiKey(HttpServletRequest request, String chave) {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {

            return;

        }



        apiKeyService.autenticarPorChave(chave).ifPresent(usuario -> {

            log.debug("API Key autenticada para o usuário: {}", usuario.getUsername());

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(

                    usuario,

                    null,

                    authoritiesComMarkerReadOnly(usuario)

            );

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);

        });

    }



    private void autenticarJwt(HttpServletRequest request, String jwt) {

        try {

            String login = jwtService.extractLogin(jwt);

            log.debug("Token JWT extraído para o usuário: {}", login);



            if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = this.userDetailsService.loadUserByUsername(login);

                log.debug("UserDetails carregado para o usuário: {}", login);



                if (jwtService.isTokenValid(jwt, userDetails)) {

                    log.debug("Token JWT válido para o usuário: {}", login);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(

                            userDetails,

                            null,

                            userDetails.getAuthorities()

                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Autenticação configurada no SecurityContext para o usuário: {}", login);

                } else {

                    log.error("Token JWT inválido para o usuário: {}", login);

                }

            }

        } catch (Exception e) {

            log.error("Erro ao processar token JWT: {}", e.getMessage(), e);

        }

    }



    static List<GrantedAuthority> authoritiesComMarkerReadOnly(Usuario usuario) {

        List<GrantedAuthority> authorities = new ArrayList<>(usuario.getAuthorities());

        authorities.add(new SimpleGrantedAuthority(ApiKeySecurity.ROLE_API_KEY_READONLY));

        return authorities;

    }

}


