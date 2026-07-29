package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.LoginDTO;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final String LOGIN = "usuario.teste";
    private static final String SENHA = "senha123";
    private static final String MENSAGEM_GENERICA = "Usuário ou senha inválidos";

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void authenticate_loginInexistente_lancaMensagemGenerica() {
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> authenticationService.authenticate(new LoginDTO(LOGIN, SENHA)));

        assertEquals(MENSAGEM_GENERICA, ex.getMessage());
    }

    @Test
    void authenticate_senhaIncorreta_lancaMesmaMensagemGenerica() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA, usuario.getSenha())).thenReturn(false);

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> authenticationService.authenticate(new LoginDTO(LOGIN, SENHA)));

        assertEquals(MENSAGEM_GENERICA, ex.getMessage());
    }

    @Test
    void authenticate_falhaPosCredencial_lancaMensagemGenerica() {
        Usuario usuario = usuarioAtivo();
        UserDetails userDetails = User.withUsername(LOGIN).password("hash").authorities(List.of()).build();

        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(SENHA, usuario.getSenha())).thenReturn(true);
        when(userDetailsService.loadUserByUsername(LOGIN)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenThrow(new RuntimeException("JWT indisponível"));

        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> authenticationService.authenticate(new LoginDTO(LOGIN, SENHA)));

        assertEquals(MENSAGEM_GENERICA, ex.getMessage());
    }

    private Usuario usuarioAtivo() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin(LOGIN);
        usuario.setSenha("hash-armazenado");
        usuario.setAtivo(true);
        return usuario;
    }
}
