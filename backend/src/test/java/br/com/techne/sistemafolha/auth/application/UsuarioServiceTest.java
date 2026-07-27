package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.UsuarioDTO;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long FUNCIONARIO_ID = 10L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FuncionarioConsultaPort funcionarioConsultaPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void atualizar_vincula_funcionario_via_port() {
        Usuario usuario = usuarioExistente();
        Funcionario funcionario = funcionarioAtivo(FUNCIONARIO_ID);
        UsuarioDTO dto = dtoComFuncionario(FUNCIONARIO_ID);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioConsultaPort.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioDTO result = usuarioService.atualizar(USUARIO_ID, dto);

        assertEquals(FUNCIONARIO_ID, result.funcionarioId());
        assertEquals(funcionario, usuario.getFuncionario());
        verify(funcionarioConsultaPort).findById(FUNCIONARIO_ID);
    }

    @Test
    void atualizar_funcionario_nao_encontrado_lanca_excecao() {
        Usuario usuario = usuarioExistente();
        UsuarioDTO dto = dtoComFuncionario(FUNCIONARIO_ID);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(funcionarioConsultaPort.findById(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        assertThrows(FuncionarioNotFoundException.class,
            () -> usuarioService.atualizar(USUARIO_ID, dto));
        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuarioExistente() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin("gestor");
        usuario.setNome("Gestor");
        usuario.setPermissoes(List.of("USER"));
        usuario.setAtivo(true);
        return usuario;
    }

    private Funcionario funcionarioAtivo(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("João Silva");
        funcionario.setCpf("12345678901");
        funcionario.setAtivo(true);
        return funcionario;
    }

    private UsuarioDTO dtoComFuncionario(Long funcionarioId) {
        return new UsuarioDTO(
            USUARIO_ID,
            "gestor",
            null,
            "Gestor",
            List.of("USER"),
            funcionarioId,
            null,
            null
        );
    }
}
