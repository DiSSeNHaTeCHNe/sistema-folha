package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.FuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.api.FuncionarioStatusFiltro;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CargoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.infrastructure.CargoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private CentroCustoRepository centroCustoRepository;
    @Mock
    private UsuarioLookupPort usuarioLookupPort;
    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 5L;

    @Test
    void listarParaUsuario_scoped_filtraPorCentrosCusto() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
                .thenReturn(contextoRestrito(Set.of(793L, 825L)));
        Funcionario inScope = funcionarioComCentroCusto(793L);
        Funcionario outScope = funcionarioComCentroCusto(999L);
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(List.of(inScope, outScope));

        List<FuncionarioDTO> result = funcionarioService.listarParaUsuario(
                LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        assertEquals(1, result.size());
        assertEquals(793L, result.get(0).centroCustoId());
    }

    @Test
    void listarParaUsuario_scoped_centrosCustoIdsVazio_retornaListaVazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
                .thenReturn(contextoRestrito(Collections.emptySet()));

        List<FuncionarioDTO> result = funcionarioService.listarParaUsuario(
                LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        assertEquals(0, result.size());
        verify(funcionarioRepository, never()).findByFiltros(any(), any(), any(), any(), any());
    }

    @Test
    void listarParaUsuario_centroCustoQueryForaEscopo_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
                .thenReturn(contextoRestrito(Set.of(793L, 825L)));

        List<FuncionarioDTO> result = funcionarioService.listarParaUsuario(
                LOGIN, null, null, 999L, null, FuncionarioStatusFiltro.ATIVO);

        assertEquals(0, result.size());
        verify(funcionarioRepository, never()).findByFiltros(any(), any(), any(), any(), any());
    }

    @Test
    void buscarPorIdParaUsuario_outOfScope_lancaFuncionarioNotFoundException() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        Funcionario outScope = funcionarioComCentroCusto(999L);
        outScope.setId(42L);
        when(funcionarioRepository.findById(42L)).thenReturn(Optional.of(outScope));

        assertThrows(FuncionarioNotFoundException.class,
                () -> funcionarioService.buscarPorIdParaUsuario(LOGIN, 42L));
    }

    @Test
    void listarParaUsuario_acessoTotal_delegaListarGlobal() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
                .thenReturn(contextoAcessoTotal());
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(List.of(funcionarioAtivo()));

        List<FuncionarioDTO> result = funcionarioService.listarParaUsuario(
                LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        assertEquals(1, result.size());
    }

    @Test
    void listarParaUsuario_semCentroCusto_excluiFuncionario() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
                .thenReturn(contextoRestrito(Set.of(793L)));
        Funcionario semCc = funcionarioAtivo();
        semCc.setCentroCusto(null);
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(List.of(semCc));

        List<FuncionarioDTO> result = funcionarioService.listarParaUsuario(
                LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        assertEquals(0, result.size());
    }

    @Test
    void cadastrar_rejeita_cpf_ativo_duplicado() {
        FuncionarioDTO dto = dtoBase("12345678901", "MAT001");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("12345678901")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> funcionarioService.cadastrar(dto));

        assertEquals("Já existe um funcionário ativo com este CPF", ex.getMessage());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void cadastrar_rejeita_id_externo_duplicado() {
        FuncionarioDTO dto = dtoBase("12345678901", "MAT001");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("12345678901")).thenReturn(false);
        when(funcionarioRepository.existsByIdExterno("MAT001")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> funcionarioService.cadastrar(dto));

        assertEquals("Já existe um funcionário com este ID externo (matrícula)", ex.getMessage());
    }

    @Test
    void cadastrar_permite_mesmo_cpf_quando_nao_ha_ativo() {
        FuncionarioDTO dto = dtoBase("12345678901", "MAT002");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("12345678901")).thenReturn(false);
        when(funcionarioRepository.existsByIdExterno("MAT002")).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCustoAtivo()));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        FuncionarioDTO result = funcionarioService.cadastrar(dto);

        assertEquals(2L, result.id());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    void listar_default_ativo_passes_ativo_true_to_repository() {
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(Collections.emptyList());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        verify(funcionarioRepository).findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true));
    }

    @Test
    void listar_inativo_passes_ativo_false_to_repository() {
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(Collections.emptyList());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.INATIVO);

        verify(funcionarioRepository).findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(false));
    }

    @Test
    void listar_todos_passes_ativo_null_to_repository() {
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.TODOS);

        verify(funcionarioRepository).findByFiltros(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void listar_with_nome_and_status_combined() {
        when(funcionarioRepository.findByFiltros(eq("%Maria%"), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(List.of());

        funcionarioService.listar("Maria", null, null, null, FuncionarioStatusFiltro.INATIVO);

        verify(funcionarioRepository).findByFiltros(eq("%Maria%"), isNull(), isNull(), isNull(), eq(false));
    }

    @Test
    void remover_sets_ativo_false() {
        Funcionario funcionario = funcionarioAtivo();
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.save(funcionario)).thenReturn(funcionario);

        funcionarioService.remover(1L);

        assertFalse(funcionario.getAtivo());
        verify(funcionarioRepository).save(funcionario);
    }

    @Test
    void segundo_remover_throws_funcionario_not_found_exception() {
        Funcionario funcionario = funcionarioAtivo();
        funcionario.setAtivo(false);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        assertThrows(FuncionarioNotFoundException.class, () -> funcionarioService.remover(1L));
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void buscarPorId_retornaDtoQuandoAtivo() {
        Funcionario funcionario = funcionarioAtivo();
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        FuncionarioDTO result = funcionarioService.buscarPorId(1L);

        assertEquals(1L, result.id());
        assertEquals("Maria Teste", result.nome());
        assertEquals("Analista", result.cargoDescricao());
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoInativo() {
        Funcionario funcionario = funcionarioAtivo();
        funcionario.setAtivo(false);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        assertThrows(FuncionarioNotFoundException.class, () -> funcionarioService.buscarPorId(1L));
    }

    @Test
    void atualizar_alteraDadosComMesmoCpf() {
        Funcionario funcionario = funcionarioAtivo();
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.existsByIdExternoAndIdNot("MAT001", 1L)).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCustoAtivo()));
        when(funcionarioRepository.save(funcionario)).thenReturn(funcionario);

        FuncionarioDTO dto = new FuncionarioDTO(
            1L, "Maria Atualizada", "12345678901", LocalDate.of(2024, 1, 15),
            1L, "Analista", 1L, "TI", 1L, "Software", "MAT001", true);
        FuncionarioDTO result = funcionarioService.atualizar(1L, dto);

        assertEquals("Maria Atualizada", result.nome());
        verify(funcionarioRepository, never()).findByCpfAndAtivoTrue(any());
    }

    @Test
    void cadastrar_idExternoEmBranco_normalizaParaNull() {
        FuncionarioDTO dto = dtoBase("98765432100", "   ");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("98765432100")).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCustoAtivo()));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(3L);
            return f;
        });

        FuncionarioDTO result = funcionarioService.cadastrar(dto);

        assertEquals(3L, result.id());
        verify(funcionarioRepository, never()).existsByIdExterno(any());
    }

    @Test
    void listar_nomeEmBranco_ignoraFiltroNome() {
        when(funcionarioRepository.findByFiltros(null, null, null, null, true))
            .thenReturn(List.of(funcionarioAtivo()));

        assertEquals(1, funcionarioService.listar("   ", null, null, null, FuncionarioStatusFiltro.ATIVO).size());
    }

    @Test
    void listar_statusTodos_passaAtivoNull() {
        when(funcionarioRepository.findByFiltros(null, null, null, null, null))
            .thenReturn(List.of(funcionarioAtivo()));

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.TODOS);
        verify(funcionarioRepository).findByFiltros(null, null, null, null, null);
    }

    @Test
    void listarParaUsuario_acessoNegado_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(), null, null, null, null));

        assertTrue(funcionarioService.listarParaUsuario(
            LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO).isEmpty());
    }

    @Test
    void listarParaUsuario_centrosNull_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertTrue(funcionarioService.listarParaUsuario(
            LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO).isEmpty());
    }

    @Test
    void listarParaUsuario_comNome_filtraComPattern() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(funcionarioRepository.findByFiltros("%Maria%", null, null, null, true))
            .thenReturn(List.of(funcionarioAtivo()));

        assertEquals(1, funcionarioService.listarParaUsuario(
            LOGIN, "Maria", null, null, null, FuncionarioStatusFiltro.ATIVO).size());
    }

    @Test
    void buscarPorIdParaUsuario_acessoNegado_lancaExcecao() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, false, false, Set.of(), null, null, null, null));

        assertThrows(FuncionarioNotFoundException.class, () ->
            funcionarioService.buscarPorIdParaUsuario(LOGIN, 1L));
    }

    @Test
    void buscarPorIdParaUsuario_acessoTotal_delega() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionarioAtivo()));

        assertEquals(1L, funcionarioService.buscarPorIdParaUsuario(LOGIN, 1L).id());
    }

    @Test
    void atualizar_cpfAlterado_validaUnicidade() {
        Funcionario existente = funcionarioAtivo();
        Funcionario outro = new Funcionario();
        outro.setId(2L);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(funcionarioRepository.findByCpfAndAtivoTrue("99999999999")).thenReturn(Optional.of(outro));
        FuncionarioDTO dto = new FuncionarioDTO(
            1L, "Maria", "99999999999", LocalDate.of(2024, 1, 15),
            1L, "Analista", 1L, "TI", 1L, "Software", "MAT001", true);

        assertThrows(IllegalArgumentException.class, () -> funcionarioService.atualizar(1L, dto));
    }

    @Test
    void cadastrar_cargoInativo_lancaExcecao() {
        Cargo inativo = cargoAtivo();
        inativo.setAtivo(false);
        when(funcionarioRepository.existsByCpfAndAtivoTrue(any())).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(inativo));

        assertThrows(CargoNotFoundException.class, () -> funcionarioService.cadastrar(dtoBase("11111111111", null)));
    }

    @Test
    void cadastrar_centroCustoInativo_lancaExcecao() {
        CentroCusto inativo = centroCustoAtivo();
        inativo.setAtivo(false);
        when(funcionarioRepository.existsByCpfAndAtivoTrue(any())).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(inativo));

        assertThrows(CentroCustoNotFoundException.class, () -> funcionarioService.cadastrar(dtoBase("11111111111", null)));
    }

    @Test
    void listar_comNome_aplicaPattern() {
        when(funcionarioRepository.findByFiltros("%Maria%", null, null, null, true))
            .thenReturn(List.of(funcionarioAtivo()));

        assertEquals(1, funcionarioService.listar("Maria", null, null, null, FuncionarioStatusFiltro.ATIVO).size());
    }

    @Test
    void buscarPorIdParaUsuario_scopedNoEscopo_retornaDto() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionarioComCentroCusto(10L)));

        assertEquals(1L, funcionarioService.buscarPorIdParaUsuario(LOGIN, 1L).id());
    }

    @Test
    void cadastrar_idExternoNull_naoValidaDuplicidade() {
        when(funcionarioRepository.existsByCpfAndAtivoTrue("11111111111")).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCustoAtivo()));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(5L);
            return f;
        });

        funcionarioService.cadastrar(dtoBase("11111111111", null));

        verify(funcionarioRepository, never()).existsByIdExterno(any());
    }

    @Test
    void atualizar_idExternoConflito_lancaExcecao() {
        Funcionario existente = funcionarioAtivo();
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(funcionarioRepository.existsByIdExternoAndIdNot("OUTRO", 1L)).thenReturn(true);
        FuncionarioDTO dto = new FuncionarioDTO(
            1L, "Maria", "12345678901", LocalDate.of(2024, 1, 15),
            1L, "Analista", 1L, "TI", 1L, "Software", "OUTRO", true);

        assertThrows(IllegalArgumentException.class, () -> funcionarioService.atualizar(1L, dto));
    }

    @Test
    void listarParaUsuario_semFuncionarioVinculado_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, true, false, Set.of(10L), null, 2L, "TI", 1));

        assertTrue(funcionarioService.listarParaUsuario(
            LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO).isEmpty());
        verify(funcionarioRepository, never()).findByFiltros(any(), any(), any(), any(), any());
    }

    @Test
    void listar_statusInativoViaResolver_passaFalse() {
        when(funcionarioRepository.findByFiltros(null, null, null, null, false)).thenReturn(List.of());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.INATIVO);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, false);
    }

    @Test
    void listarParaUsuario_nomeNull_naoAplicaPattern() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(funcionarioRepository.findByFiltros(null, null, null, null, true)).thenReturn(List.of());

        funcionarioService.listarParaUsuario(LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, true);
    }

    @Test
    void listarParaUsuario_semOrganograma_acessoNegado() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, false, false, Set.of(10L), null, 2L, "TI", 1));

        assertTrue(funcionarioService.listarParaUsuario(
            LOGIN, null, null, null, null, FuncionarioStatusFiltro.ATIVO).isEmpty());
    }

    @Test
    void cadastrar_cpfDuplicadoViaExistsByCpf_lancaExcecao() {
        when(funcionarioRepository.existsByCpfAndAtivoTrue("11111111111")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () ->
            funcionarioService.cadastrar(dtoBase("11111111111", null)));
    }

    @Test
    void listarParaUsuario_statusInativo_passaFalse() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(funcionarioRepository.findByFiltros(null, null, null, null, false)).thenReturn(List.of());

        funcionarioService.listarParaUsuario(
            LOGIN, null, null, null, null, FuncionarioStatusFiltro.INATIVO);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, false);
    }

    @Test
    void listarParaUsuario_nomeVazio_naoAplicaPattern() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(funcionarioRepository.findByFiltros(null, null, null, null, true)).thenReturn(List.of());

        funcionarioService.listarParaUsuario(
            LOGIN, "   ", null, null, null, FuncionarioStatusFiltro.ATIVO);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, true);
    }

    @Test
    void listarParaUsuario_centroCustoIdForaEscopo_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));

        assertTrue(funcionarioService.listarParaUsuario(
            LOGIN, null, null, 99L, null, FuncionarioStatusFiltro.ATIVO).isEmpty());
    }

    @Test
    void listar_statusNull_passaAtivoTrue() {
        when(funcionarioRepository.findByFiltros(null, null, null, null, true))
            .thenReturn(List.of());

        funcionarioService.listar(null, null, null, null, null);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, true);
    }

    @Test
    void listarParaUsuario_centroCustoIdNoEscopo_consultaRepositorio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(funcionarioRepository.findByFiltros(null, null, 793L, null, true))
            .thenReturn(List.of(funcionarioComCentroCusto(793L)));

        List<FuncionarioDTO> result = funcionarioService.listarParaUsuario(
            LOGIN, null, null, 793L, null, FuncionarioStatusFiltro.ATIVO);

        assertEquals(1, result.size());
        verify(funcionarioRepository).findByFiltros(null, null, 793L, null, true);
    }

    @Test
    void listarParaUsuario_scoped_comNome_aplicaPattern() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(funcionarioRepository.findByFiltros("%Maria%", null, null, null, true))
            .thenReturn(List.of(funcionarioComCentroCusto(793L)));

        assertEquals(1, funcionarioService.listarParaUsuario(
            LOGIN, "Maria", null, null, null, FuncionarioStatusFiltro.ATIVO).size());
    }

    @Test
    void atualizar_cpfAlterado_mesmoFuncionario_naoConflita() {
        Funcionario existente = funcionarioAtivo();
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(funcionarioRepository.findByCpfAndAtivoTrue("99999999999"))
            .thenReturn(Optional.of(existente));
        when(funcionarioRepository.existsByIdExternoAndIdNot("MAT001", 1L)).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCustoAtivo()));
        when(funcionarioRepository.save(existente)).thenReturn(existente);

        FuncionarioDTO dto = new FuncionarioDTO(
            1L, "Maria", "99999999999", LocalDate.of(2024, 1, 15),
            1L, "Analista", 1L, "TI", 1L, "Software", "MAT001", true);

        assertEquals("99999999999", funcionarioService.atualizar(1L, dto).cpf());
    }

    @Test
    void listarParaUsuario_statusNull_passaAtivoTrue() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(funcionarioRepository.findByFiltros(null, null, null, null, true))
            .thenReturn(List.of(funcionarioComCentroCusto(793L)));

        funcionarioService.listarParaUsuario(LOGIN, null, null, null, null, null);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, true);
    }

    @Test
    void listarParaUsuario_scoped_nomeVazio_naoAplicaPattern() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(793L)));
        when(funcionarioRepository.findByFiltros(null, null, null, null, true))
            .thenReturn(List.of(funcionarioComCentroCusto(793L)));

        funcionarioService.listarParaUsuario(LOGIN, "   ", null, null, null, FuncionarioStatusFiltro.ATIVO);

        verify(funcionarioRepository).findByFiltros(null, null, null, null, true);
    }

    @Test
    void aplicarFiltroAcesso_acessoTotal_retornaTrue() throws Exception {
        var method = FuncionarioService.class.getDeclaredMethod(
            "aplicarFiltroAcesso", Funcionario.class, AccessContextDTO.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(
            funcionarioService, funcionarioAtivo(), contextoAcessoTotal());

        assertTrue(result);
    }

    @Test
    void aplicarFiltroAcesso_semVinculoOrganograma_retornaFalse() throws Exception {
        var method = FuncionarioService.class.getDeclaredMethod(
            "aplicarFiltroAcesso", Funcionario.class, AccessContextDTO.class);
        method.setAccessible(true);
        AccessContextDTO contexto = new AccessContextDTO(false, true, false, Set.of(10L), null, 2L, "TI", 1);

        boolean result = (boolean) method.invoke(
            funcionarioService, funcionarioAtivo(), contexto);

        assertFalse(result);
    }

    @Test
    void aplicarFiltroAcesso_semNoOrganograma_retornaFalse() throws Exception {
        var method = FuncionarioService.class.getDeclaredMethod(
            "aplicarFiltroAcesso", Funcionario.class, AccessContextDTO.class);
        method.setAccessible(true);
        AccessContextDTO contexto = new AccessContextDTO(true, false, false, Set.of(10L), null, 2L, "TI", 1);

        boolean result = (boolean) method.invoke(
            funcionarioService, funcionarioAtivo(), contexto);

        assertFalse(result);
    }

    @Test
    void aplicarFiltroAcesso_funcionarioNull_retornaFalse() throws Exception {
        var method = FuncionarioService.class.getDeclaredMethod(
            "aplicarFiltroAcesso", Funcionario.class, AccessContextDTO.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(
            funcionarioService, null, contextoRestrito(Set.of(10L)));

        assertFalse(result);
    }

    private Funcionario funcionarioAtivo() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("Maria Teste");
        funcionario.setCpf("12345678901");
        funcionario.setDataAdmissao(LocalDate.of(2024, 1, 15));
        funcionario.setCargo(cargoAtivo());
        funcionario.setCentroCusto(centroCustoAtivo());
        funcionario.setAtivo(true);
        return funcionario;
    }

    private FuncionarioDTO dtoBase(String cpf, String idExterno) {
        return new FuncionarioDTO(
                null,
                "Maria Teste",
                cpf,
                LocalDate.of(2024, 1, 15),
                1L,
                "Analista",
                1L,
                "TI",
                1L,
                "Software",
                idExterno,
                true
        );
    }

    private Cargo cargoAtivo() {
        Cargo cargo = new Cargo();
        cargo.setId(1L);
        cargo.setDescricao("Analista");
        cargo.setAtivo(true);
        return cargo;
    }

    private CentroCusto centroCustoAtivo() {
        LinhaNegocio ln = new LinhaNegocio();
        ln.setId(1L);
        ln.setDescricao("Software");
        CentroCusto cc = new CentroCusto();
        cc.setId(1L);
        cc.setDescricao("TI");
        cc.setAtivo(true);
        cc.setLinhaNegocio(ln);
        return cc;
    }

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
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

    private Funcionario funcionarioComCentroCusto(Long ccId) {
        Funcionario funcionario = funcionarioAtivo();
        LinhaNegocio ln = new LinhaNegocio();
        ln.setId(1L);
        ln.setDescricao("Software");
        CentroCusto cc = new CentroCusto();
        cc.setId(ccId);
        cc.setDescricao("CC " + ccId);
        cc.setAtivo(true);
        cc.setLinhaNegocio(ln);
        funcionario.setCentroCusto(cc);
        return funcionario;
    }
}
