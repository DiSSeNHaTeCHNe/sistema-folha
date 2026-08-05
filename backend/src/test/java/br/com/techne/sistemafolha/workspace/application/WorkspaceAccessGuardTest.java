package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
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
class WorkspaceAccessGuardTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private WorkspaceAccessGuard workspaceAccessGuard;

    @Test
    void assertEscopo_acessoTotal_naoLanca() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotal());

        workspaceAccessGuard.assertEscopo(LOGIN);
    }

    @Test
    void assertEscopo_restritoComCentros_naoLanca() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));

        workspaceAccessGuard.assertEscopo(LOGIN);
    }

    @Test
    void assertEscopo_semFuncionario_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(
                false, false, false, Set.of(), MotivoNegacaoAcesso.SEM_FUNCIONARIO, null, null, null));

        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_semOrganograma_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, true, false, Set.of(10L), null, null, null, null));

        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_centrosVazios_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_centrosNull_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_motivoNegacaoExplicito_lanca403() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(),
                MotivoNegacaoAcesso.SEM_FUNCIONARIO, null, null, null));

        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo(LOGIN));
    }

    @Test
    void assertEscopo_loginAusente_lanca403() {
        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo(null));
        verify(usuarioLookupPort, never()).findByLoginAndAtivoTrue(LOGIN);
    }

    @Test
    void assertEscopo_usuarioInexistente_lanca403() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue("ghost")).thenReturn(Optional.empty());

        assertThrows(WorkspaceAcessoNegadoException.class, () -> workspaceAccessGuard.assertEscopo("ghost"));
        verify(organogramaAcessoPort, never()).obterContextoAcesso(USUARIO_ID);
    }

    @Test
    void resolve_acessoTotal_retornaUsuarioIdEContexto() {
        stubUsuario();
        AccessContextDTO contexto = contextoAcessoTotal();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contexto);

        WorkspaceAccessGuard.ResolvedWorkspaceAccess access = workspaceAccessGuard.resolve(LOGIN);

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

        WorkspaceAccessGuard.ResolvedWorkspaceAccess access = workspaceAccessGuard.resolve(LOGIN);

        assertFalse(access.denied());
        assertEquals(centros, access.centrosScoped());
    }

    @Test
    void resolve_negado_retornaDenied() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        WorkspaceAccessGuard.ResolvedWorkspaceAccess access = workspaceAccessGuard.resolve(LOGIN);

        assertTrue(access.denied());
        assertNull(access.usuarioId());
    }

    @Test
    void deveNegarAcesso_acessoTotal_retornaFalse() {
        assertFalse(workspaceAccessGuard.deveNegarAcesso(contextoAcessoTotal()));
    }

    @Test
    void deveNegarAcesso_centrosVazios_retornaTrue() {
        assertTrue(workspaceAccessGuard.deveNegarAcesso(
            new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1)));
    }

    @Test
    void podeVerTemplate_acessoTotal_retornaTrue() {
        AccessContextDTO contexto = contextoAcessoTotal();

        assertTrue(workspaceAccessGuard.podeVerTemplate(contexto, 99L, 1L));
        verify(organogramaAcessoPort, never()).noEstaNaSubarvore(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void podeVerTemplate_viewerNaSubarvore_retornaTrue() {
        AccessContextDTO contexto = contextoRestrito(Set.of(10L));
        when(organogramaAcessoPort.noEstaNaSubarvore(2L, 5L)).thenReturn(true);

        assertTrue(workspaceAccessGuard.podeVerTemplate(contexto, 99L, 5L));
    }

    @Test
    void podeVerTemplate_viewerForaSubarvore_retornaFalse() {
        AccessContextDTO contexto = contextoRestrito(Set.of(10L));
        when(organogramaAcessoPort.noEstaNaSubarvore(2L, 5L)).thenReturn(false);

        assertFalse(workspaceAccessGuard.podeVerTemplate(contexto, 99L, 5L));
    }

    @Test
    void podeVerTemplate_semNoOrganogramaTemplate_retornaFalse() {
        AccessContextDTO contexto = contextoRestrito(Set.of(10L));

        assertFalse(workspaceAccessGuard.podeVerTemplate(contexto, 99L, null));
    }

    @Test
    void assertPodeVerTemplate_foraSubarvore_lanca403() {
        AccessContextDTO contexto = contextoRestrito(Set.of(10L));
        when(organogramaAcessoPort.noEstaNaSubarvore(2L, 5L)).thenReturn(false);

        assertThrows(WorkspaceAcessoNegadoException.class,
            () -> workspaceAccessGuard.assertPodeVerTemplate(contexto, 99L, 5L));
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
