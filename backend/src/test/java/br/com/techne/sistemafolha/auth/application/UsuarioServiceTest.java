package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.api.UsuarioDTO;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.domain.UsuarioNotFoundException;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
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
import java.util.Set;

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

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private UsuarioService usuarioService;

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_LOOKUP_ID = 5L;

    @Test
    void listarParaUsuario_scoped_excluiUsuariosSemFuncionario() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        Usuario comFuncionario = usuarioComFuncionarioCc(793L);
        Usuario semFuncionario = usuarioExistente();
        when(usuarioRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(List.of(comFuncionario, semFuncionario));

        List<UsuarioDTO> result = usuarioService.listarParaUsuario(LOGIN, null, null, null);

        assertEquals(1, result.size());
        assertEquals(FUNCIONARIO_ID, result.get(0).funcionarioId());
    }

    @Test
    void listarParaUsuario_scoped_centrosCustoIdsVazio_retornaListaVazia() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoRestrito(Collections.emptySet()));

        List<UsuarioDTO> result = usuarioService.listarParaUsuario(LOGIN, null, null, null);

        assertEquals(0, result.size());
        verify(usuarioRepository, never()).findByFiltros(any(), any(), any());
    }

    @Test
    void listarParaUsuario_scoped_excluiFuncionarioForaEscopo() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        Usuario inScope = usuarioComFuncionarioCc(793L);
        Usuario outScope = usuarioComFuncionarioCc(999L);
        when(usuarioRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(List.of(inScope, outScope));

        List<UsuarioDTO> result = usuarioService.listarParaUsuario(LOGIN, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void buscarPorIdParaUsuario_outOfScope_lancaUsuarioNotFoundException() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        Usuario outScope = usuarioComFuncionarioCc(999L);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(outScope));

        assertThrows(UsuarioNotFoundException.class,
                () -> usuarioService.buscarPorIdParaUsuario(LOGIN, USUARIO_ID));
    }

    @Test
    void listarParaUsuario_acessoTotal_delegaListarGlobal() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoAcessoTotal());
        when(usuarioRepository.findByFiltros(isNull(), isNull(), isNull()))
                .thenReturn(List.of(usuarioExistente()));

        List<UsuarioDTO> result = usuarioService.listarParaUsuario(LOGIN, null, null, null);

        assertEquals(1, result.size());
    }

    @Test
    void buscarPorLoginParaUsuario_semFuncionario_lancaUsuarioNotFoundException() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        when(usuarioRepository.findByLoginAndAtivoTrue("admin")).thenReturn(Optional.of(usuarioExistente()));

        assertThrows(UsuarioNotFoundException.class,
                () -> usuarioService.buscarPorLoginParaUsuario(LOGIN, "admin"));
    }

    @Test
    void buscarPorFuncionarioParaUsuario_foraEscopo_retornaNull() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        Funcionario outScope = funcionarioComCentroCusto(999L);
        when(funcionarioConsultaPort.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(outScope));

        assertNull(usuarioService.buscarPorFuncionarioParaUsuario(LOGIN, FUNCIONARIO_ID));
    }

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

    @Test
    void listarParaUsuario_acessoNegado_retornaVazio() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(), null, null, null, null));

        assertTrue(usuarioService.listarParaUsuario(LOGIN, null, null, null).isEmpty());
    }

    @Test
    void listarParaUsuario_scoped_comNomeLogin_aplicaPatterns() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        Usuario comFuncionario = usuarioComFuncionarioCc(793L);
        when(usuarioRepository.findByFiltros("%Gest%", "%ges%", null)).thenReturn(List.of(comFuncionario));

        assertEquals(1, usuarioService.listarParaUsuario(LOGIN, "Gest", "ges", null).size());
    }

    @Test
    void buscarPorIdParaUsuario_acessoNegado_lancaExcecao() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(), null, null, null, null));

        assertThrows(UsuarioNotFoundException.class, () ->
            usuarioService.buscarPorIdParaUsuario(LOGIN, USUARIO_ID));
    }

    @Test
    void buscarPorIdParaUsuario_acessoTotal_delega() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID)).thenReturn(contextoAcessoTotal());
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuarioExistente()));

        assertEquals(USUARIO_ID, usuarioService.buscarPorIdParaUsuario(LOGIN, USUARIO_ID).id());
    }

    @Test
    void buscarPorLoginParaUsuario_acessoTotal_delega() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID)).thenReturn(contextoAcessoTotal());
        when(usuarioRepository.findByLoginAndAtivoTrue("gestor")).thenReturn(Optional.of(usuarioExistente()));

        assertEquals("gestor", usuarioService.buscarPorLoginParaUsuario(LOGIN, "gestor").login());
    }

    @Test
    void buscarPorFuncionarioParaUsuario_acessoTotal_delega() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID)).thenReturn(contextoAcessoTotal());
        when(usuarioRepository.findByFuncionarioIdAndAtivoTrue(FUNCIONARIO_ID))
            .thenReturn(Optional.of(usuarioComFuncionarioCc(793L)));

        assertEquals(FUNCIONARIO_ID, usuarioService.buscarPorFuncionarioParaUsuario(
            LOGIN, FUNCIONARIO_ID).funcionarioId());
    }

    @Test
    void buscarPorFuncionarioParaUsuario_acessoNegado_retornaNull() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(), null, null, null, null));

        assertNull(usuarioService.buscarPorFuncionarioParaUsuario(LOGIN, FUNCIONARIO_ID));
    }

    @Test
    void cadastrar_comFuncionarioId_vinculaFuncionario() {
        when(usuarioRepository.existsByLoginAndAtivoTrue("novo")).thenReturn(false);
        when(funcionarioConsultaPort.findById(FUNCIONARIO_ID)).thenReturn(Optional.of(funcionarioAtivo(FUNCIONARIO_ID)));
        when(passwordEncoder.encode("senha")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UsuarioDTO dto = new UsuarioDTO(null, "novo", "senha", "Novo", List.of("USER"), FUNCIONARIO_ID, null, null);
        assertEquals(FUNCIONARIO_ID, usuarioService.cadastrar(dto).funcionarioId());
    }

    @Test
    void atualizar_loginAlteradoSemConflito_persiste() {
        Usuario usuario = usuarioExistente();
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByLoginAndAtivoTrue("gestor2")).thenReturn(false);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioDTO dto = new UsuarioDTO(USUARIO_ID, "gestor2", null, "Gestor", List.of("USER"), null, null, null);
        assertEquals("gestor2", usuarioService.atualizar(USUARIO_ID, dto).login());
        verify(usuarioRepository).existsByLoginAndAtivoTrue("gestor2");
    }

    @Test
    void usuarioNoEscopo_funcionarioNull_retornaFalse() throws Exception {
        var method = UsuarioService.class.getDeclaredMethod(
            "usuarioNoEscopo", Usuario.class, AccessContextDTO.class);
        method.setAccessible(true);
        Usuario semFuncionario = usuarioExistente();

        assertFalse((boolean) method.invoke(
            usuarioService, semFuncionario, contextoRestrito(Set.of(10L))));
    }

    @Test
    void listarParaUsuario_scoped_nomeLoginVazios_naoAplicaPattern() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(usuarioRepository.findByFiltros(null, null, null)).thenReturn(List.of());

        usuarioService.listarParaUsuario(LOGIN, "   ", "   ", null);

        verify(usuarioRepository).findByFiltros(null, null, null);
    }

    @Test
    void buscarPorLoginParaUsuario_acessoNegado_lancaExcecao() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(), null, null, null, null));

        assertThrows(UsuarioNotFoundException.class, () ->
            usuarioService.buscarPorLoginParaUsuario(LOGIN, "outro"));
    }

    @Test
    void buscarPorLoginParaUsuario_scopedNoEscopo_retornaDto() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(usuarioRepository.findByLoginAndAtivoTrue("gestor"))
            .thenReturn(Optional.of(usuarioComFuncionarioCc(793L)));

        assertEquals("gestor", usuarioService.buscarPorLoginParaUsuario(LOGIN, "gestor").login());
    }

    @Test
    void buscarPorFuncionarioParaUsuario_funcionarioInexistente_retornaNull() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(funcionarioConsultaPort.findById(FUNCIONARIO_ID)).thenReturn(Optional.empty());

        assertNull(usuarioService.buscarPorFuncionarioParaUsuario(LOGIN, FUNCIONARIO_ID));
    }

    @Test
    void atualizar_semSenha_naoReencode() {
        Usuario usuario = usuarioExistente();
        usuario.setSenha("hash-antigo");
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioDTO dto = new UsuarioDTO(USUARIO_ID, "gestor", null, "Gestor", List.of("USER"), null, null, null);
        usuarioService.atualizar(USUARIO_ID, dto);

        verify(passwordEncoder, never()).encode(any());
        assertEquals("hash-antigo", usuario.getSenha());
    }

    @Test
    void usuarioNoEscopo_acessoTotal_retornaTrue() throws Exception {
        var method = UsuarioService.class.getDeclaredMethod(
            "usuarioNoEscopo", Usuario.class, AccessContextDTO.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(
            usuarioService, usuarioExistente(), contextoAcessoTotal()));
    }

    @Test
    void usuarioNoEscopo_semOrganograma_retornaFalse() throws Exception {
        var method = UsuarioService.class.getDeclaredMethod(
            "usuarioNoEscopo", Usuario.class, AccessContextDTO.class);
        method.setAccessible(true);
        AccessContextDTO ctx = new AccessContextDTO(true, false, false, Set.of(10L), null, 2L, "TI", 1);

        assertFalse((boolean) method.invoke(
            usuarioService, usuarioComFuncionarioCc(10L), ctx));
    }

    @Test
    void funcionarioNoEscopo_semCentroCusto_retornaFalse() throws Exception {
        var method = UsuarioService.class.getDeclaredMethod(
            "funcionarioNoEscopo", Funcionario.class, AccessContextDTO.class);
        method.setAccessible(true);
        Funcionario f = funcionarioAtivo(FUNCIONARIO_ID);

        assertFalse((boolean) method.invoke(
            usuarioService, f, contextoRestrito(Set.of(10L))));
    }

    @Test
    void listarParaUsuario_centrosNull_retornaVazio() {
        stubUsuarioLookup();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_LOOKUP_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertTrue(usuarioService.listarParaUsuario(LOGIN, null, null, null).isEmpty());
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

    private void stubUsuarioLookup() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_LOOKUP_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Collections.emptySet(), null, null, null, null);
    }

    private Usuario usuarioComFuncionarioCc(Long ccId) {
        Usuario usuario = usuarioExistente();
        usuario.setFuncionario(funcionarioComCentroCusto(ccId));
        return usuario;
    }

    private Funcionario funcionarioComCentroCusto(Long ccId) {
        Funcionario funcionario = funcionarioAtivo(FUNCIONARIO_ID);
        CentroCusto cc = new CentroCusto();
        cc.setId(ccId);
        cc.setDescricao("CC " + ccId);
        cc.setAtivo(true);
        funcionario.setCentroCusto(cc);
        return funcionario;
    }
}
