package br.com.techne.sistemafolha.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET =
        "techne_engenharia_sistemas_ltda_50_737_766_0001_21_2024_2025_2026_2027_2028_2029_2030";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(Clock.systemDefaultZone());
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86_400_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604_800_000L);
    }

    @Test
    void generateToken_eValidar_roundTrip() {
        UserDetails user = userDetails("usuario.teste");

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("usuario.teste", jwtService.extractLogin(token));
        assertTrue(jwtService.isTokenValid(token, user));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void isTokenValid_usuarioDiferente_retornaFalse() {
        UserDetails emissor = userDetails("usuario.a");
        UserDetails outro = userDetails("usuario.b");
        String token = jwtService.generateToken(emissor);

        assertFalse(jwtService.isTokenValid(token, outro));
    }

    @Test
    void generateRefreshToken_retornaStringNaoVazia() {
        String refresh = jwtService.generateRefreshToken();

        assertNotNull(refresh);
        assertFalse(refresh.isBlank());
    }

    private UserDetails userDetails(String username) {
        return User.builder()
            .username(username)
            .password("secret")
            .authorities(new SimpleGrantedAuthority("ROLE_USER"))
            .build();
    }
}
