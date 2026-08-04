package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAccessGuardTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private DashboardAccessGuard dashboardAccessGuard;

    @Test
    void assertEscopo_acessoTotal_naoLanca() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotal());

        dashboardAccessGuard.assertEscopo(LOGIN);
    }

    @Test
    void assertEscopo_restritoComCentros_naoLanca() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));

        dashboardAccessGuard.assertEscopo(LOGIN);
    }

    @Test
    void assertEscopo_semFuncionario_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(
                false, false, false, Set.of(), MotivoNegacaoAcesso.SEM_FUNCIONARIO, null, null, null));

        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_semOrganograma_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, true, false, Set.of(10L), null, null, null, null));

        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_centrosVazios_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_centrosNull_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_motivoNegacaoExplicito_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(),
                MotivoNegacaoAcesso.SEM_FUNCIONARIO, null, null, null));

        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_loginAusente_lanca403() {
        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo(null));
        verify(usuarioLookupPort, never()).findByLoginAndAtivoTrue(LOGIN);
    }

    @Test
    void assertEscopo_usuarioInexistente_lanca403() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue("ghost")).thenReturn(Optional.empty());

        assertThrows(DashboardAcessoNegadoException.class, () -> dashboardAccessGuard.assertEscopo("ghost"));
        verify(organogramaAcessoPort, never()).obterContextoAcesso(USUARIO_ID);
    }

    @Test
    void resolve_acessoTotal_retornaUsuarioIdEContexto() {
        stubUsuario();
        AccessContextDTO contexto = contextoAcessoTotal();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);

        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(LOGIN);

        assertFalse(access.denied());
        assertEquals(USUARIO_ID, access.usuarioId());
        assertEquals(contexto, access.contexto());
        assertNull(access.centrosScoped());
    }

    @Test
    void resolve_restrito_retornaCentrosScoped() {
        stubUsuario();
        Set<Long> centros = Set.of(10L, 20L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(centros));

        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(LOGIN);

        assertFalse(access.denied());
        assertEquals(centros, access.centrosScoped());
    }

    @Test
    void resolve_negado_retornaDenied() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(LOGIN);

        assertTrue(access.denied());
        assertNull(access.usuarioId());
    }

    @Test
    void deveNegarAcesso_acessoTotal_retornaFalse() {
        assertFalse(dashboardAccessGuard.deveNegarAcesso(contextoAcessoTotal()));
    }

    @Test
    void deveNegarAcesso_centrosVazios_retornaTrue() {
        assertTrue(dashboardAccessGuard.deveNegarAcesso(
            new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1)));
    }

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }
}
