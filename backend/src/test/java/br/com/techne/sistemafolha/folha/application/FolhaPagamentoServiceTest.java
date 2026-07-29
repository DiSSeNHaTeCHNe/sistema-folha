package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaPagamentoServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;
    private static final LocalDate DATA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate DATA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private CadastrosLookupPort cadastrosLookupPort;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @Mock
    private FolhaTotalizacaoService folhaTotalizacaoService;

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @InjectMocks
    private FolhaPagamentoService folhaPagamentoService;

    @Test
    void consultarPorPeriodo_acesso_total_sem_vinculo_retorna_linhas_ativas() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotalEarlyReturn());

        FolhaPagamento folha = folhaAtiva(10L, 99L);
        when(folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folha));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, DATA_INICIO, DATA_FIM, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    void consultarPorPeriodo_sem_acesso_total_negado_retorna_lista_vazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        FolhaPagamento folha = folhaAtiva(10L, 99L);
        when(folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folha));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, DATA_INICIO, DATA_FIM, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void consultarPorPeriodo_comDecimoTerceiro_filtraPorTipo() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotalEarlyReturn());

        FolhaPagamento folhaDecimo = folhaAtiva(11L, 99L);
        folhaDecimo.setDecimoTerceiro(true);
        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
                DATA_INICIO, DATA_FIM, true))
            .thenReturn(List.of(folhaDecimo));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, DATA_INICIO, DATA_FIM, true);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).id());
        verify(folhaPagamentoRepository).findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
            DATA_INICIO, DATA_FIM, true);
    }

    @Test
    void consultarPorFuncionario_acesso_total_retorna_todos_registros() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaPagamento folha = folhaAtiva(10L, 99L);
        when(folhaPagamentoRepository.findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(
                99L, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folha));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorFuncionario(
            LOGIN, 99L, DATA_INICIO, DATA_FIM, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
    }

    @Test
    void consultarPorFuncionario_comDecimoTerceiroFalse_filtraSomenteFolhaRegular() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaPagamento folhaRegular = folhaAtiva(10L, 99L);
        folhaRegular.setDecimoTerceiro(false);
        when(folhaPagamentoRepository.findByFuncionarioIdAndCompetenciaAndDecimoTerceiroAndAtivoTrue(
                99L, DATA_INICIO, DATA_FIM, false))
            .thenReturn(List.of(folhaRegular));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorFuncionario(
            LOGIN, 99L, DATA_INICIO, DATA_FIM, false);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).id());
        assertFalse(result.get(0).decimoTerceiro());
        verify(folhaPagamentoRepository).findByFuncionarioIdAndCompetenciaAndDecimoTerceiroAndAtivoTrue(
            99L, DATA_INICIO, DATA_FIM, false);
        verify(folhaPagamentoRepository, never())
            .findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(any(), any(), any());
    }

    @Test
    void consultarPorFuncionario_comDecimoTerceiroTrue_filtraSomenteFolha13() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaPagamento folhaDecimo = folhaAtiva(11L, 99L);
        folhaDecimo.setDecimoTerceiro(true);
        when(folhaPagamentoRepository.findByFuncionarioIdAndCompetenciaAndDecimoTerceiroAndAtivoTrue(
                99L, DATA_INICIO, DATA_FIM, true))
            .thenReturn(List.of(folhaDecimo));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorFuncionario(
            LOGIN, 99L, DATA_INICIO, DATA_FIM, true);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).id());
        assertTrue(result.get(0).decimoTerceiro());
    }

    @Test
    void consultarPorFuncionario_acesso_restrito_filtra_por_centro_custo() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));

        FolhaPagamento permitido = folhaAtiva(1L, 99L);
        FolhaPagamento bloqueado = folhaAtiva(2L, 100L);
        CentroCusto outroCentro = new CentroCusto();
        outroCentro.setId(20L);
        bloqueado.getFuncionario().setCentroCusto(outroCentro);
        bloqueado.setCentroCusto(outroCentro);

        when(folhaPagamentoRepository.findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(
                99L, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(permitido, bloqueado));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorFuncionario(
            LOGIN, 99L, DATA_INICIO, DATA_FIM, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
    }

    @Test
    void consultarPorFuncionario_ordenar_por_rubricaCodigo_crescente() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaPagamento folha200 = folhaComRubricaCodigo(1L, 99L, "200");
        FolhaPagamento folha100 = folhaComRubricaCodigo(2L, 99L, "100");
        when(folhaPagamentoRepository.findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(
                99L, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folha200, folha100));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorFuncionario(
            LOGIN, 99L, DATA_INICIO, DATA_FIM, null);

        assertEquals(2, result.size());
        assertEquals("100", result.get(0).rubricaCodigo());
        assertEquals("200", result.get(1).rubricaCodigo());
    }

    @Test
    void consultarPorFuncionario_mesmo_rubricaCodigo_ordenar_por_id_crescente() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaPagamento folhaId20 = folhaComRubricaCodigo(20L, 99L, "100");
        FolhaPagamento folhaId10 = folhaComRubricaCodigo(10L, 99L, "100");
        when(folhaPagamentoRepository.findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(
                99L, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folhaId20, folhaId10));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorFuncionario(
            LOGIN, 99L, DATA_INICIO, DATA_FIM, null);

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).id());
        assertEquals(20L, result.get(1).id());
    }

    @Test
    void consultarPorPeriodo_duasCompetenciasCcDistintos_gestorVeSoCompetenciaDoEscopo_fcc02_fcc03() {
        // FCC-02 / FCC-03: jan CC-A (100L), fev CC-B (200L), funcionário atual CC-B
        stubUsuario();

        LocalDate janInicio = LocalDate.of(2026, 1, 1);
        LocalDate janFim = LocalDate.of(2026, 1, 31);
        LocalDate fevInicio = LocalDate.of(2026, 2, 1);
        LocalDate fevFim = LocalDate.of(2026, 2, 28);

        CentroCusto ccA = new CentroCusto();
        ccA.setId(100L);
        ccA.setDescricao("CC Alpha");
        CentroCusto ccB = new CentroCusto();
        ccB.setId(200L);
        ccB.setDescricao("CC Beta");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(50L);
        funcionario.setNome("Transferido");
        funcionario.setCentroCusto(ccB);

        FolhaPagamento folhaJan = folhaAtiva(1L, 50L);
        folhaJan.setFuncionario(funcionario);
        folhaJan.setCentroCusto(ccA);
        folhaJan.setDataInicio(janInicio);
        folhaJan.setDataFim(janFim);

        FolhaPagamento folhaFev = folhaAtiva(2L, 50L);
        folhaFev.setFuncionario(funcionario);
        folhaFev.setCentroCusto(ccB);
        folhaFev.setDataInicio(fevInicio);
        folhaFev.setDataFim(fevFim);

        when(folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(janInicio, janFim))
            .thenReturn(List.of(folhaJan));
        when(folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(fevInicio, fevFim))
            .thenReturn(List.of(folhaFev));

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(100L)));
        List<FolhaPagamentoDTO> gestorAJan = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, janInicio, janFim, null);
        assertEquals(1, gestorAJan.size());
        assertEquals(1L, gestorAJan.get(0).id());

        List<FolhaPagamentoDTO> gestorAFev = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, fevInicio, fevFim, null);
        assertTrue(gestorAFev.isEmpty());

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(200L)));
        List<FolhaPagamentoDTO> gestorBFev = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, fevInicio, fevFim, null);
        assertEquals(1, gestorBFev.size());
        assertEquals(2L, gestorBFev.get(0).id());

        List<FolhaPagamentoDTO> gestorBJan = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, janInicio, janFim, null);
        assertTrue(gestorBJan.isEmpty());
    }

    @Test
    void consultarPorPeriodo_linhaCcDiferenteDoFuncionarioAtual_filtraPorCcDaLinha_fcc06() {
        // FCC-06: linha CC-A, funcionário CC-B atual — gestor A vê, gestor B não
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(100L)));

        CentroCusto ccLinha = new CentroCusto();
        ccLinha.setId(100L);
        ccLinha.setDescricao("CC Alpha");

        CentroCusto ccFuncionarioAtual = new CentroCusto();
        ccFuncionarioAtual.setId(200L);
        ccFuncionarioAtual.setDescricao("CC Beta");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(50L);
        funcionario.setNome("Transferido");
        funcionario.setCentroCusto(ccFuncionarioAtual);

        FolhaPagamento folha = folhaAtiva(1L, 50L);
        folha.setFuncionario(funcionario);
        folha.setCentroCusto(ccLinha);

        when(folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folha));

        List<FolhaPagamentoDTO> gestorA = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, DATA_INICIO, DATA_FIM, null);
        assertEquals(1, gestorA.size());

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(200L)));
        List<FolhaPagamentoDTO> gestorB = folhaPagamentoService.consultarPorPeriodo(
            LOGIN, DATA_INICIO, DATA_FIM, null);
        assertTrue(gestorB.isEmpty());
    }

    @Test
    void consultarPorCentroCusto_sem_permissao_retorna_lista_vazia() {
        stubUsuario();
        when(organogramaAcessoPort.usuarioPodeAcessarCentroCusto(USUARIO_ID, 5L)).thenReturn(false);

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorCentroCusto(
            LOGIN, 5L, DATA_INICIO, DATA_FIM);

        assertTrue(result.isEmpty());
        verify(cadastrosLookupPort, never()).findCentroCustoById(any());
        verify(folhaPagamentoRepository, never())
            .findByCentroCustoAndDataInicioBetweenAndAtivoTrue(any(), any(), any());
    }

    @Test
    void consultarPorCentroCusto_comPermissao_consultaCcDaLinha_fcc10() {
        stubUsuario();
        when(organogramaAcessoPort.usuarioPodeAcessarCentroCusto(USUARIO_ID, 100L)).thenReturn(true);

        CentroCusto ccConsulta = new CentroCusto();
        ccConsulta.setId(100L);
        ccConsulta.setDescricao("CC Alpha");
        when(cadastrosLookupPort.findCentroCustoById(100L)).thenReturn(Optional.of(ccConsulta));

        FolhaPagamento folhaCcAntigo = folhaAtiva(1L, 50L);
        folhaCcAntigo.getCentroCusto().setId(100L);
        CentroCusto ccFuncionarioAtual = new CentroCusto();
        ccFuncionarioAtual.setId(200L);
        folhaCcAntigo.getFuncionario().setCentroCusto(ccFuncionarioAtual);

        when(folhaPagamentoRepository.findByCentroCustoAndDataInicioBetweenAndAtivoTrue(
                ccConsulta, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folhaCcAntigo));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorCentroCusto(
            LOGIN, 100L, DATA_INICIO, DATA_FIM);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).centroCustoId());
        verify(folhaPagamentoRepository).findByCentroCustoAndDataInicioBetweenAndAtivoTrue(
            ccConsulta, DATA_INICIO, DATA_FIM);
        verify(folhaPagamentoRepository, never())
            .findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(any(), any(), any());
    }

    @Test
    void consultarPorCentroCusto_comPermissao_retorna_linhas() {
        stubUsuario();
        when(organogramaAcessoPort.usuarioPodeAcessarCentroCusto(USUARIO_ID, 5L)).thenReturn(true);

        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(5L);
        when(cadastrosLookupPort.findCentroCustoById(5L)).thenReturn(Optional.of(centroCusto));

        FolhaPagamento folha = folhaAtiva(10L, 99L);
        when(folhaPagamentoRepository.findByCentroCustoAndDataInicioBetweenAndAtivoTrue(
                centroCusto, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(folha));

        List<FolhaPagamentoDTO> result = folhaPagamentoService.consultarPorCentroCusto(
            LOGIN, 5L, DATA_INICIO, DATA_FIM);

        assertEquals(1, result.size());
        verify(folhaPagamentoRepository).findByCentroCustoAndDataInicioBetweenAndAtivoTrue(
            centroCusto, DATA_INICIO, DATA_FIM);
    }

    @Test
    void consultarTotaisPorFuncionario_delega_totalizacao_via_port_apos_filtro_acl() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());

        FolhaLinhaSnapshot linha = new FolhaLinhaSnapshot(
            99L, "João", 10L, "TI", null, null, 5L, "Analista",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000"),
            (short) 1, (short) 1, (short) 1, br.com.techne.sistemafolha.folha.domain.OrigemLinha.FOLHA_ADP, null);
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(DATA_INICIO, DATA_FIM, false, null))
            .thenReturn(List.of(linha));
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioAndCompetenciaFimAndDecimoTerceiroAndAtivoTrue(
                DATA_INICIO, DATA_FIM, false))
            .thenReturn(List.of());

        FolhaTotaisFuncionarioDTO total = new FolhaTotaisFuncionarioDTO(
            99L, "João", DATA_INICIO, DATA_FIM,
            5L, "Analista", 10L, "TI", null, null,
            1, 0,
            new BigDecimal("5000"), new BigDecimal("4000"), new BigDecimal("5000"),
            BigDecimal.ZERO, new BigDecimal("500.00"), new BigDecimal("5500.00"));
        when(folhaTotalizacaoService.calcularTotaisPorFuncionario(
                List.of(linha), contextoAcessoTotal(), BigDecimal.ZERO, DATA_INICIO, DATA_FIM))
            .thenReturn(List.of(total));

        List<FolhaTotaisFuncionarioDTO> result = folhaPagamentoService.consultarTotaisPorFuncionario(
            LOGIN, DATA_INICIO, DATA_FIM, false);

        assertEquals(1, result.size());
        assertEquals(99L, result.get(0).funcionarioId());
        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(DATA_INICIO, DATA_FIM, false, null);
        verify(folhaTotalizacaoService).calcularTotaisPorFuncionario(
            eq(List.of(linha)), eq(contextoAcessoTotal()), eq(BigDecimal.ZERO), eq(DATA_INICIO), eq(DATA_FIM));
    }

    @Test
    void removerSeAutorizado_folha_fora_do_centro_retorna_false() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(99L)));
        when(folhaPagamentoRepository.findById(3L)).thenReturn(Optional.of(folhaAtiva(3L, 99L)));

        assertFalse(folhaPagamentoService.removerSeAutorizado(LOGIN, 3L));
        verify(folhaPagamentoRepository, never()).softDelete(any());
    }

    @Test
    void removerSeAutorizado_com_acesso_executa_soft_delete() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        when(folhaPagamentoRepository.findById(3L)).thenReturn(Optional.of(folhaAtiva(3L, 99L)));

        assertTrue(folhaPagamentoService.removerSeAutorizado(LOGIN, 3L));
        verify(folhaPagamentoRepository).softDelete(3L);
    }

    @Test
    void removerSeAutorizado_acesso_total_executa_soft_delete() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(folhaPagamentoRepository.findById(3L)).thenReturn(Optional.of(folhaAtiva(3L, 99L)));

        assertTrue(folhaPagamentoService.removerSeAutorizado(LOGIN, 3L));
        verify(folhaPagamentoRepository).softDelete(3L);
    }

    @Test
    void removerSeAutorizado_registro_inexistente_retorna_false() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(folhaPagamentoRepository.findById(999L)).thenReturn(Optional.empty());

        assertFalse(folhaPagamentoService.removerSeAutorizado(LOGIN, 999L));
        verify(folhaPagamentoRepository, never()).softDelete(any());
    }

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    /** Shape of OrganogramaAcessoService early-return for ACESSO_TOTAL without funcionario/nó. */
    private AccessContextDTO contextoAcessoTotalEarlyReturn() {
        return new AccessContextDTO(
            false,
            false,
            true,
            Collections.emptySet(),
            null,
            null,
            null,
            null
        );
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private AccessContextDTO contextoNegado(MotivoNegacaoAcesso motivo) {
        return new AccessContextDTO(false, false, false, Set.of(), motivo, null, null, null);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private FolhaPagamento folhaComRubricaCodigo(Long id, Long funcionarioId, String rubricaCodigo) {
        FolhaPagamento folha = folhaAtiva(id, funcionarioId);
        folha.getRubrica().setCodigo(rubricaCodigo);
        return folha;
    }

    private FolhaPagamento folhaAtiva(Long id, Long funcionarioId) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(10L);
        centroCusto.setDescricao("TI");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(funcionarioId);
        funcionario.setNome("João Silva");
        funcionario.setCentroCusto(centroCusto);

        TipoRubrica tipoRubrica = new TipoRubrica();
        tipoRubrica.setDescricao("Provento");

        Rubrica rubrica = new Rubrica();
        rubrica.setId(1L);
        rubrica.setCodigo("1001");
        rubrica.setDescricao("Salário");
        rubrica.setTipoRubrica(tipoRubrica);

        Cargo cargo = new Cargo();
        cargo.setId(5L);
        cargo.setDescricao("Analista");

        FolhaPagamento folha = new FolhaPagamento();
        folha.setId(id);
        folha.setFuncionario(funcionario);
        folha.setRubrica(rubrica);
        folha.setCargo(cargo);
        folha.setCentroCusto(centroCusto);
        folha.setDataInicio(DATA_INICIO);
        folha.setDataFim(DATA_FIM);
        folha.setValor(new BigDecimal("5000"));
        folha.setQuantidade(BigDecimal.ONE);
        folha.setBaseCalculo(new BigDecimal("5000"));
        folha.setAtivo(true);
        return folha;
    }
}
