package br.com.techne.sistemafolha.config;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import br.com.techne.sistemafolha.security.JwtAuthenticationFilter;
import br.com.techne.sistemafolha.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String TIPO_BENEFICIO = "/tipo-beneficio";
    private static final String TIPO_BENEFICIO_ALL = "/tipo-beneficio/**";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ApiKeyService apiKeyService;

    public SecurityConfig(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            ApiKeyService apiKeyService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.apiKeyService = apiKeyService;
    }

    @Bean
    @SuppressWarnings({"java:S4502", "java:S1192"}) // JWT stateless API; ROLE_ADMIN via hasRole(ROLE_ADMIN)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // NOSONAR java:S4502
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .requestMatchers("/usuarios/**").authenticated()
                .requestMatchers("/funcionarios/**").authenticated()
                .requestMatchers("/rubricas/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/folha-pagamento/processar").hasRole(ROLE_ADMIN)
                .requestMatchers("/folha-pagamento/**").authenticated()
                .requestMatchers("/beneficio-mensal/**").authenticated()
                .requestMatchers("/importacao/**").authenticated()
                .requestMatchers("/funcionario-rubrica-fixa/**").hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.GET, TIPO_BENEFICIO, TIPO_BENEFICIO_ALL).authenticated()
                .requestMatchers(HttpMethod.POST, TIPO_BENEFICIO).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.PUT, TIPO_BENEFICIO_ALL).hasRole(ROLE_ADMIN)
                .requestMatchers(HttpMethod.DELETE, TIPO_BENEFICIO_ALL).hasRole(ROLE_ADMIN)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService, apiKeyService);
    }
} 