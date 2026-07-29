package br.com.techne.sistemafolha.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class JwtSecretStartupValidator {

    static final String DEFAULT_JWT_SECRET =
        "techne_engenharia_sistemas_ltda_50_737_766_0001_21_2024_2025_2026_2027_2028_2029_2030";

    private static final Logger logger = LoggerFactory.getLogger(JwtSecretStartupValidator.class);

    private final String jwtSecret;
    private final Environment environment;

    public JwtSecretStartupValidator(
            @Value("${jwt.secret}") String jwtSecret,
            Environment environment) {
        this.jwtSecret = jwtSecret;
        this.environment = environment;
    }

    @PostConstruct
    void validateJwtSecret() {
        if (!DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            return;
        }
        if (isProdProfile()) {
            throw new IllegalStateException(
                "JWT_SECRET não configurado: defina a variável de ambiente JWT_SECRET em produção");
        }
        logger.warn(
            "JWT_SECRET usando valor default do application.yml — permitido apenas em desenvolvimento");
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equals);
    }
}
