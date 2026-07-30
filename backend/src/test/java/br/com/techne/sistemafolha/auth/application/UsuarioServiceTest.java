package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.UsuarioDTO;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.domain.UsuarioNotFoundException;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    void listar_sem_filtros_delegates_to_repository() {
        when(usuarioRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        usuarioService.listar(null, null, null);

        verify(usuarioRepository).findByFiltros(isNull(), isNull(), isNull());
    }

    @Test
    void listar_nome_passes_ilike_pattern_to_repository() {
        when(usuarioRepository.findByFiltros(eq("%Maria%"), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        usuarioService.listar("Maria", null, null);

        verify(usuarioRepository).findByFiltros(eq("%Maria%"), isNull(), isNull());
    }

    @Test
    void listar_login_passes_ilike_pattern_to_repository() {
        when(usuarioRepository.findByFiltros(isNull(), eq("%adm%"), isNull()))
                .thenReturn(Collections.emptyList());

        usuarioService.listar(null, "adm", null);

        verify(usuarioRepository).findByFiltros(isNull(), eq("%adm%"), isNull());
    }

    @Test
    void listar_funcionarioId_passes_exact_match_to_repository() {
        when(usuarioRepository.findByFiltros(isNull(), isNull(), eq(FUNCIONARIO_ID)))
                .thenReturn(Collections.emptyList());

        usuarioService.listar(null, null, FUNCIONARIO_ID);

        verify(usuarioRepository).findByFiltros(isNull(), isNull(), eq(FUNCIONARIO_ID));
    }

    @Test
    void listar_trim_ignora_espacos_em_branco() {
        when(usuarioRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        usuarioService.listar("   ", "  ", null);

        verify(usuarioRepository).findByFiltros(isNull(), isNull(), isNull());
    }

    @Test
    void listar_combined_filters_delegates_to_repository() {
        when(usuarioRepository.findByFiltros(eq("%Maria%"), eq("%adm%"), eq(FUNCIONARIO_ID)))
                .thenReturn(Collections.emptyList());

        usuarioService.listar("Maria", "adm", FUNCIONARIO_ID);

        verify(usuarioRepository).findByFiltros(eq("%Maria%"), eq("%adm%"), eq(FUNCIONARIO_ID));
    }

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

    @Test
    void listarTodos_delegatesToListarSemFiltros() {
        when(usuarioRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(List.of(usuarioExistente()));

        List<UsuarioDTO> result = usuarioService.listarTodos();

        assertEquals(1, result.size());
        assertEquals("gestor", result.get(0).login());
    }

    @Test
    void buscarPorId_quandoAtivo_retornaDto() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioExistente()));

        UsuarioDTO result = usuarioService.buscarPorId(USUARIO_ID);

        assertEquals(USUARIO_ID, result.id());
        assertEquals("gestor", result.login());
    }

    @Test
    void buscarPorId_inexistente_lancaUsuarioNotFoundException() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(UsuarioNotFoundException.class, () -> usuarioService.buscarPorId(USUARIO_ID));
    }

    @Test
    void buscarPorLogin_quandoAtivo_retornaDto() {
        when(usuarioRepository.findByLoginAndAtivoTrue("gestor")).thenReturn(Optional.of(usuarioExistente()));

        UsuarioDTO result = usuarioService.buscarPorLogin("gestor");

        assertEquals("gestor", result.login());
    }

    @Test
    void buscarPorLogin_inexistente_lancaUsuarioNotFoundException() {
        when(usuarioRepository.findByLoginAndAtivoTrue("inexistente")).thenReturn(Optional.empty());

        assertThrows(UsuarioNotFoundException.class, () -> usuarioService.buscarPorLogin("inexistente"));
    }

    @Test
    void buscarPorFuncionario_quandoExiste_retornaDto() {
        Usuario usuario = usuarioExistente();
        usuario.setFuncionario(funcionarioAtivo(FUNCIONARIO_ID));
        when(usuarioRepository.findByFuncionarioIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.of(usuario));

        UsuarioDTO result = usuarioService.buscarPorFuncionario(FUNCIONARIO_ID);

        assertEquals(FUNCIONARIO_ID, result.funcionarioId());
    }

    @Test
    void buscarPorFuncionario_quandoNaoExiste_retornaNull() {
        when(usuarioRepository.findByFuncionarioIdAndAtivoTrue(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        assertNull(usuarioService.buscarPorFuncionario(FUNCIONARIO_ID));
    }

    @Test
    void cadastrar_persisteUsuarioComSenhaCriptografada() {
        UsuarioDTO dto = new UsuarioDTO(null, "novo", "senha123", "Novo Usuário", List.of("USER"), null, null, null);
        when(usuarioRepository.existsByLoginAndAtivoTrue("novo")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-novo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario saved = inv.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        UsuarioDTO result = usuarioService.cadastrar(dto);

        assertEquals(99L, result.id());
        assertEquals("novo", result.login());
        verify(passwordEncoder).encode("senha123");
    }

    @Test
    void cadastrar_loginDuplicado_lancaIllegalArgumentException() {
        UsuarioDTO dto = new UsuarioDTO(null, "novo", "senha123", "Novo Usuário", List.of("USER"), null, null, null);
        when(usuarioRepository.existsByLoginAndAtivoTrue("novo")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.cadastrar(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void atualizar_alteraSenhaQuandoInformada() {
        Usuario usuario = usuarioExistente();
        UsuarioDTO dto = new UsuarioDTO(USUARIO_ID, "gestor", "novaSenha", "Gestor", List.of("USER"), null, null, null);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha")).thenReturn("hash-nova");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.atualizar(USUARIO_ID, dto);

        assertEquals("hash-nova", usuario.getSenha());
    }

    @Test
    void atualizar_loginDuplicado_lancaIllegalArgumentException() {
        Usuario usuario = usuarioExistente();
        UsuarioDTO dto = new UsuarioDTO(USUARIO_ID, "outro", null, "Gestor", List.of("USER"), null, null, null);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByLoginAndAtivoTrue("outro")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.atualizar(USUARIO_ID, dto));
    }

    @Test
    void atualizar_removeFuncionarioQuandoIdNull() {
        Usuario usuario = usuarioExistente();
        usuario.setFuncionario(funcionarioAtivo(FUNCIONARIO_ID));
        UsuarioDTO dto = new UsuarioDTO(USUARIO_ID, "gestor", null, "Gestor", List.of("USER"), null, null, null);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.atualizar(USUARIO_ID, dto);

        assertNull(usuario.getFuncionario());
    }

    @Test
    void remover_desativaUsuario() {
        Usuario usuario = usuarioExistente();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.remover(USUARIO_ID);

        assertFalse(usuario.isAtivo());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void alterarSenha_senhaAtualCorreta_atualizaSenha() {
        Usuario usuario = usuarioExistente();
        usuario.setSenha("hash-atual");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("atual", "hash-atual")).thenReturn(true);
        when(passwordEncoder.encode("nova")).thenReturn("hash-nova");
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        usuarioService.alterarSenha(USUARIO_ID, "atual", "nova");

        verify(passwordEncoder).encode("nova");
        assertEquals("hash-nova", usuario.getSenha());
    }

    @Test
    void alterarSenha_senhaAtualIncorreta_lancaRuntimeException() {
        Usuario usuario = usuarioExistente();
        usuario.setSenha("hash-atual");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "hash-atual")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> usuarioService.alterarSenha(USUARIO_ID, "errada", "nova"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void verificarSenha_delegatesToPasswordEncoder() {
        when(passwordEncoder.matches("texto", "hash")).thenReturn(true);

        assertTrue(usuarioService.verificarSenha("texto", "hash"));
    }

    @Test
    void verificarSenha_naoLogaSenhaEmTexto() {
        when(passwordEncoder.matches("segredo", "hash")).thenReturn(true);
        ListAppender<ILoggingEvent> appender = capturarLogsUsuarioService();

        usuarioService.verificarSenha("segredo", "hash");

        assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains("segredo")));
        assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().contains("hash")));
    }

    private ListAppender<ILoggingEvent> capturarLogsUsuarioService() {
        Logger logger = (Logger) LoggerFactory.getLogger(UsuarioService.class);
        logger.setLevel(ch.qos.logback.classic.Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
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
