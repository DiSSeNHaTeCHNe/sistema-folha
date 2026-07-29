package br.com.techne.sistemafolha.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtSecretStartupValidatorTest {

    @Mock
    private Environment environment;

    @Test
    void validateJwtSecret_prodComDefaultSecret_falhaStartup() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(
            JwtSecretStartupValidator.DEFAULT_JWT_SECRET, environment);

        assertThrows(IllegalStateException.class,
            () -> ReflectionTestUtils.invokeMethod(validator, "validateJwtSecret"));
    }

    @Test
    void validateJwtSecret_devComDefaultSecret_permiteComWarn() {
        when(environment.getActiveProfiles()).thenReturn(new String[] {});
        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(
            JwtSecretStartupValidator.DEFAULT_JWT_SECRET, environment);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(validator, "validateJwtSecret"));
    }

    @Test
    void validateJwtSecret_prodComSecretCustomizado_permite() {
        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(
            "custom-production-secret-value", environment);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(validator, "validateJwtSecret"));
    }
}
