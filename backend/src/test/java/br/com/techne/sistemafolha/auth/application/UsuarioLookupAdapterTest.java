package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioLookupAdapterTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioLookupAdapter adapter;

    @Test
    void findById_usuarioPresente_retornaOptionalComUsuario() {
        Usuario usuario = usuario(1L, "admin");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        Optional<Usuario> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void findById_usuarioAusente_retornaOptionalVazio() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Usuario> result = adapter.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByLoginAndAtivoTrue_usuarioPresente_retornaOptionalComUsuario() {
        Usuario usuario = usuario(2L, "operador");
        when(usuarioRepository.findByLoginAndAtivoTrue("operador"))
            .thenReturn(Optional.of(usuario));

        Optional<Usuario> result = adapter.findByLoginAndAtivoTrue("operador");

        assertTrue(result.isPresent());
        assertEquals("operador", result.get().getLogin());
    }

    @Test
    void findByLoginAndAtivoTrue_usuarioAusente_retornaOptionalVazio() {
        when(usuarioRepository.findByLoginAndAtivoTrue("inexistente"))
            .thenReturn(Optional.empty());

        Optional<Usuario> result = adapter.findByLoginAndAtivoTrue("inexistente");

        assertTrue(result.isEmpty());
    }

    private Usuario usuario(Long id, String login) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setLogin(login);
        return usuario;
    }
}
