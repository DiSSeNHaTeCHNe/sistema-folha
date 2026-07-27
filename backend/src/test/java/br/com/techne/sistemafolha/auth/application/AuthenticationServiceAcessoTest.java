package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.AcessoUsuarioDTO;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceAcessoTest {

    private static final Long USUARIO_ID = 10L;
    private static final String LOGIN = "usuario.teste";

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
    void obterAcessoUsuario_acessoTotal_mapeiaFlagAcessoTotal() {
        AccessContextDTO contexto = new AccessContextDTO(
            false,
            false,
            true,
            Collections.emptySet(),
            null,
            null,
            null,
            null
        );
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);

        AcessoUsuarioDTO acesso = authenticationService.obterAcessoUsuario(USUARIO_ID);

        assertTrue(acesso.isAcessoTotal());
        assertFalse(acesso.isTemFuncionarioVinculado());
        assertFalse(acesso.isTemNoOrganograma());
        assertTrue(acesso.getCentrosCustoIds().isEmpty());
        assertNull(acesso.getMotivoNegacao());
    }

    @Test
    void obterAcessoUsuario_semFuncionario_mapeiaMotivoNegacaoSemFuncionario() {
        AccessContextDTO contexto = new AccessContextDTO(
            false,
            false,
            false,
            Collections.emptySet(),
            MotivoNegacaoAcesso.SEM_FUNCIONARIO,
            null,
            null,
            null
        );
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);

        AcessoUsuarioDTO acesso = authenticationService.obterAcessoUsuario(USUARIO_ID);

        assertFalse(acesso.isTemFuncionarioVinculado());
        assertFalse(acesso.isTemNoOrganograma());
        assertFalse(acesso.isAcessoTotal());
        assertTrue(acesso.getCentrosCustoIds().isEmpty());
        assertEquals(MotivoNegacaoAcesso.SEM_FUNCIONARIO, acesso.getMotivoNegacao());
        assertEquals(0, acesso.getQuantidadeCentrosAcessiveis());
    }

    @Test
    void obterAcessoUsuarioPorLogin_grantParcial_mapeiaCentrosCustoIds() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);

        AccessContextDTO contexto = new AccessContextDTO(
            true,
            true,
            false,
            Set.of(100L, 200L),
            null,
            5L,
            "Diretoria",
            1
        );

        when(usuarioRepository.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);

        AcessoUsuarioDTO acesso = authenticationService.obterAcessoUsuarioPorLogin(LOGIN);

        assertTrue(acesso.isTemFuncionarioVinculado());
        assertTrue(acesso.isTemNoOrganograma());
        assertFalse(acesso.isAcessoTotal());
        assertEquals(Set.of(100L, 200L), acesso.getCentrosCustoIds());
        assertNull(acesso.getMotivoNegacao());
        assertEquals(5L, acesso.getNoOrganogramaId());
        assertEquals("Diretoria", acesso.getNoOrganogramaNome());
        assertEquals(1, acesso.getNivel());
        assertEquals(2, acesso.getQuantidadeCentrosAcessiveis());
    }
}
