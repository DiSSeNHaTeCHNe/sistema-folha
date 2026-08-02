package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumoFolhaPagamentoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);
    private static final LocalDateTime DATA_IMPORTACAO = LocalDateTime.of(2024, 11, 1, 8, 0);
    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;
    private static final Long CENTRO_A = 10L;

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @InjectMocks
    private ResumoFolhaPagamentoService resumoFolhaPagamentoService;

    private static final LocalDate ANO_2024_INICIO = LocalDate.of(2024, 1, 1);
    private static final LocalDate ANO_2024_FIM = LocalDate.of(2024, 12, 31);

    @Test
    void listarTodos_mapeia_resumos_ativos() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        stubGlobalSemLinhas();
        ResumoFolhaPagamento resumo = resumoAtivo(1L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(resumo));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(new BigDecimal("50000.00"), result.get(0).totalLiquido());
        assertEquals(new BigDecimal("60000.00"), result.get(0).totalBruto());
    }

    @Test
    void consultarPorCompetencia_retorna_optional_quando_encontrado() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        stubGlobalSemLinhas();
        ResumoFolhaPagamento resumo = resumoAtivo(2L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(Optional.of(resumo));

        Optional<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.consultarPorCompetencia(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().id());
    }

    /** RSF-01 + RSF-05: scoped totais from lines in user's CCs; ≠ global snapshot; encargos 0. */
    @Test
    void listarTodos_scoped_agregaLinhasDoCentro_totaisDiferentesDoSnapshot_encargosZero() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));
        stubBeneficiosVazios();

        ResumoFolhaPagamento snapshot = resumoAtivo(1L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(CENTRO_A)))
            .thenReturn(List.of(
                linha(101L, CENTRO_A, "PROVENTO", "5000.00"),
                linha(101L, CENTRO_A, "DESCONTO", "500.00"),
                linha(102L, CENTRO_A, "PROVENTO", "3000.00"),
                linha(102L, CENTRO_A, "DESCONTO", "200.00")
            ));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(1, result.size());
        ResumoFolhaPagamentoDTO dto = result.get(0);
        assertEquals(1L, dto.id());
        assertEquals(2, dto.totalEmpregados());
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalPagamentos()));
        assertEquals(0, new BigDecimal("700.00").compareTo(dto.totalDescontos()));
        assertEquals(0, new BigDecimal("7300.00").compareTo(dto.totalLiquido()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalBruto()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalCustoEmpresa()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalEncargos()));
        // RSF-05: would fail if scoped mirrored global snapshot
        assertNotEquals(snapshot.getTotalEmpregados(), dto.totalEmpregados());
        assertNotEquals(0, snapshot.getTotalPagamentos().compareTo(dto.totalPagamentos()));
        assertNotEquals(0, snapshot.getTotalLiquido().compareTo(dto.totalLiquido()));
        assertNotEquals(0, snapshot.getTotalEncargos().compareTo(dto.totalEncargos()));
    }

    /** FIX2-08/FIX2-16/FIX2-24: global com linhas agrega via motor sem rateio; encargos informativos do snapshot. */
    @Test
    void listarTodos_acessoTotal_comLinhas_agregaSemRateio_totalBrutoOperadorBased() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        stubBeneficiosVazios();

        ResumoFolhaPagamento snapshot = resumoAtivo(5L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null))
            .thenReturn(List.of(
                linha(101L, CENTRO_A, "PROVENTO", "5000.00"),
                linha(101L, CENTRO_A, "DESCONTO", "500.00"),
                linha(102L, CENTRO_A, "PROVENTO", "3000.00"),
                linha(102L, CENTRO_A, "DESCONTO", "200.00")
            ));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(1, result.size());
        ResumoFolhaPagamentoDTO dto = result.get(0);
        assertEquals(5L, dto.id());
        assertEquals(2, dto.totalEmpregados());
        assertEquals(0, new BigDecimal("10000.00").compareTo(dto.totalEncargos()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalCustoEmpresa()));
        assertNotEquals(0, dto.totalEncargos().compareTo(BigDecimal.ZERO));
        assertNotEquals(0, dto.totalEncargos().add(dto.totalCustoEmpresa())
            .compareTo(dto.totalCustoEmpresa()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalPagamentos()));
        assertEquals(0, new BigDecimal("700.00").compareTo(dto.totalDescontos()));
        assertEquals(0, new BigDecimal("7300.00").compareTo(dto.totalLiquido()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalBruto()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(dto.totalCustoEmpresa()));
        assertNotEquals(0, snapshot.getTotalPagamentos().compareTo(dto.totalBruto()));
        assertNotEquals(0, snapshot.getTotalLiquido().compareTo(dto.totalLiquido()));
    }

    /** FIX2-24 fallback: sem linhas operador-based → snapshot ADP legado (encargos informativos). */
    @Test
    void listarTodos_acessoTotal_semLinhas_retornaSnapshotPersistidoComEncargosInformativos() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        stubGlobalSemLinhas();
        ResumoFolhaPagamento snapshot = resumoAtivo(5L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(1, result.size());
        ResumoFolhaPagamentoDTO dto = result.get(0);
        assertEquals(5L, dto.id());
        assertEquals(100, dto.totalEmpregados());
        assertEquals(new BigDecimal("10000.00"), dto.totalEncargos());
        assertEquals(new BigDecimal("60000.00"), dto.totalPagamentos());
        assertEquals(new BigDecimal("10000.00"), dto.totalDescontos());
        assertEquals(new BigDecimal("50000.00"), dto.totalLiquido());
        assertEquals(new BigDecimal("60000.00"), dto.totalBruto());
        assertEquals(new BigDecimal("60000.00"), dto.totalCustoEmpresa());
        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null);
    }

    /** RSF-03: deny (no funcionario/nó) → empty list. */
    @Test
    void listarTodos_acessoNegado_semFuncionario_retornaListaVazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertTrue(result.isEmpty());
        verify(resumoFolhaPagamentoRepository, never()).findByCompetenciaInicioBetweenAndAtivoTrue(any(), any());
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), anyBoolean(), any());
    }

    /** RSF-03: deny (empty centros, no ACESSO_TOTAL) → empty list. */
    @Test
    void listarTodos_acessoNegado_centrosVazios_retornaListaVazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Collections.emptySet()));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertTrue(result.isEmpty());
        verify(resumoFolhaPagamentoRepository, never()).findByCompetenciaInicioBetweenAndAtivoTrue(any(), any());
    }

    /** RSF-04: competência in snapshot, no lines in user scope → zeros + metadata preserved. */
    @Test
    void listarTodos_scoped_semLinhasNoEscopo_retornaZerosPreservandoMetadados() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));
        stubBeneficiosVazios();

        ResumoFolhaPagamento snapshot = resumoAtivo(7L);
        snapshot.setDecimoTerceiro(true);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, true, Set.of(CENTRO_A)))
            .thenReturn(List.of());

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(1, result.size());
        ResumoFolhaPagamentoDTO dto = result.get(0);
        assertEquals(7L, dto.id());
        assertEquals(0, dto.totalEmpregados());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalPagamentos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalDescontos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalLiquido()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalEncargos()));
        assertEquals(COMPETENCIA_INICIO, dto.competenciaInicio());
        assertEquals(COMPETENCIA_FIM, dto.competenciaFim());
        assertTrue(dto.decimoTerceiro());
        assertEquals(DATA_IMPORTACAO, dto.dataImportacao());
        assertTrue(dto.ativo());
    }

    /** RSF-04 via competencia endpoint + RSF-05 discrimination (lines only in other CC). */
    @Test
    void consultarPorCompetencia_scoped_linhasSoEmOutroCentro_zerosComIdDoSnapshot() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));
        stubBeneficiosVazios();

        ResumoFolhaPagamento snapshot = resumoAtivo(9L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(Optional.of(snapshot));
        // Port already filters by CC — empty for CENTRO_A (lines only exist in B)
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM), eq(false), eq(Set.of(CENTRO_A))))
            .thenReturn(List.of());

        Optional<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.consultarPorCompetencia(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(result.isPresent());
        ResumoFolhaPagamentoDTO dto = result.get();
        assertEquals(9L, dto.id());
        assertEquals(0, dto.totalEmpregados());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.totalLiquido()));
        assertNotEquals(snapshot.getTotalEmpregados(), dto.totalEmpregados());
    }

    /** DT13-03: scoped com normal e 13º no mesmo mês → totais distintos por resumo. */
    @Test
    void listarTodos_scoped_mesComNormalE13_retornaTotaisDistintos() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));
        stubBeneficiosVazios();

        ResumoFolhaPagamento snapshotRegular = resumoAtivo(10L);
        snapshotRegular.setDecimoTerceiro(false);
        ResumoFolhaPagamento snapshotDecimo = resumoAtivo(11L);
        snapshotDecimo.setDecimoTerceiro(true);

        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshotRegular, snapshotDecimo));

        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(CENTRO_A)))
            .thenReturn(List.of(linha(101L, CENTRO_A, "PROVENTO", "5000.00")));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, true, Set.of(CENTRO_A)))
            .thenReturn(List.of(linha(101L, CENTRO_A, "PROVENTO", "2000.00")));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(2, result.size());
        ResumoFolhaPagamentoDTO regular = result.stream()
            .filter(d -> !Boolean.TRUE.equals(d.decimoTerceiro()))
            .findFirst()
            .orElseThrow();
        ResumoFolhaPagamentoDTO decimo = result.stream()
            .filter(d -> Boolean.TRUE.equals(d.decimoTerceiro()))
            .findFirst()
            .orElseThrow();
        assertEquals(0, new BigDecimal("5000.00").compareTo(regular.totalLiquido()));
        assertEquals(0, new BigDecimal("2000.00").compareTo(decimo.totalLiquido()));
        assertNotEquals(0, regular.totalLiquido().compareTo(decimo.totalLiquido()));
    }

    /** FOLH-01: ano omitido → default ano corrente via consultarPorPeriodo. */
    @Test
    void listarTodos_anoOmitido_usaAnoCorrente() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        int anoCorrente = LocalDate.now().getYear();
        LocalDate inicio = LocalDate.of(anoCorrente, 1, 1);
        LocalDate fim = LocalDate.of(anoCorrente, 12, 31);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(inicio, fim))
            .thenReturn(List.of());

        resumoFolhaPagamentoService.listarTodos(LOGIN, null, null);

        verify(resumoFolhaPagamentoRepository).findByCompetenciaInicioBetweenAndAtivoTrue(inicio, fim);
    }

    /** FOLH-02: ano + mes restringe ao mês via consultarPorPeriodo. */
    @Test
    void listarTodos_anoEMes_filtraPorMes() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        stubGlobalSemLinhas();
        LocalDate inicio = LocalDate.of(2024, 10, 1);
        LocalDate fim = LocalDate.of(2024, 10, 31);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(inicio, fim))
            .thenReturn(List.of(resumoAtivo(3L)));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, 10);

        assertEquals(1, result.size());
        verify(resumoFolhaPagamentoRepository).findByCompetenciaInicioBetweenAndAtivoTrue(inicio, fim);
    }

    /** FOLH-03: ano sem resumos → lista vazia. */
    @Test
    void listarTodos_anoSemResumos_retornaListaVazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                LocalDate.of(2099, 1, 1), LocalDate.of(2099, 12, 31)))
            .thenReturn(List.of());

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2099, null);

        assertTrue(result.isEmpty());
    }

    /** FOLH-03 + ACL: deny com filtro ano → lista vazia sem consultar repositório. */
    @Test
    void listarTodos_acessoNegado_comAno_retornaListaVazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertTrue(result.isEmpty());
        verify(resumoFolhaPagamentoRepository, never()).findByCompetenciaInicioBetweenAndAtivoTrue(any(), any());
    }

    /** FOLH-04: ano fora do intervalo → IllegalArgumentException. */
    @Test
    void listarTodos_anoInvalido_lancaIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> resumoFolhaPagamentoService.listarTodos(LOGIN, 1999, null));

        assertEquals("Ano deve estar entre 2000 e 2100", ex.getMessage());
        verify(resumoFolhaPagamentoRepository, never()).findByCompetenciaInicioBetweenAndAtivoTrue(any(), any());
    }

    /** FCLT-ACL-06: scoped agrega apenas linhas do escopo via port. */
    @Test
    void listarTodos_scoped_agregaSomenteLinhasDoEscopo() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));
        stubBeneficiosVazios();

        ResumoFolhaPagamento snapshot = resumoAtivo(12L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(CENTRO_A)))
            .thenReturn(List.of(linha(101L, CENTRO_A, "PROVENTO", "1000.00")));

        resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(CENTRO_A));
    }

    @Test
    void listarMaisRecentes_acessoNegado_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA));

        assertTrue(resumoFolhaPagamentoService.listarMaisRecentes(LOGIN).isEmpty());
        verify(resumoFolhaPagamentoRepository, never()).findLatestResumos();
    }

    @Test
    void listarMaisRecentes_acessoTotal_mapeiaResumos() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        stubGlobalSemLinhas();
        when(resumoFolhaPagamentoRepository.findLatestResumos()).thenReturn(List.of(resumoAtivo(20L)));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarMaisRecentes(LOGIN);

        assertEquals(1, result.size());
        assertEquals(20L, result.get(0).id());
    }

    @Test
    void consultarPorCompetencia_acessoNegado_retornaEmpty() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        assertTrue(resumoFolhaPagamentoService.consultarPorCompetencia(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
        verify(resumoFolhaPagamentoRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void consultarPorPeriodo_acessoNegado_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA));

        assertTrue(resumoFolhaPagamentoService.consultarPorPeriodo(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void listarTodos_usuarioNaoEncontrado_lancaRuntimeException() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
            resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null));
    }

    @Test
    void listarTodos_scoped_comBeneficios_agregaCustoEmpresa() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(CENTRO_A)));
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of(101L, new BigDecimal("200.00")));

        ResumoFolhaPagamento snapshot = resumoAtivo(30L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioBetweenAndAtivoTrue(
                ANO_2024_INICIO, ANO_2024_FIM))
            .thenReturn(List.of(snapshot));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(CENTRO_A)))
            .thenReturn(List.of(linha(101L, CENTRO_A, "PROVENTO", "5000.00")));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null);

        assertEquals(0, new BigDecimal("5200.00").compareTo(result.get(0).totalCustoEmpresa()));
    }

    @Test
    void listarTodos_ano2101_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            resumoFolhaPagamentoService.listarTodos(LOGIN, 2101, null));
    }

    @Test
    void listarTodos_acessoNegado_centrosNull_retornaListaVazia() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertTrue(resumoFolhaPagamentoService.listarTodos(LOGIN, 2024, null).isEmpty());
        verify(resumoFolhaPagamentoRepository, never()).findByCompetenciaInicioBetweenAndAtivoTrue(any(), any());
    }

    private void stubGlobalSemLinhas() {
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(any(), any(), anyBoolean(), isNull()))
            .thenReturn(List.of());
    }

    private void stubBeneficiosVazios() {
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of());
    }

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Collections.emptySet(), null, null, null, null);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private AccessContextDTO contextoNegado(MotivoNegacaoAcesso motivo) {
        return new AccessContextDTO(false, false, false, Set.of(), motivo, null, null, null);
    }

    private ResumoFolhaPagamento resumoAtivo(Long id) {
        ResumoFolhaPagamento resumo = new ResumoFolhaPagamento();
        resumo.setId(id);
        resumo.setTotalEmpregados(100);
        resumo.setTotalEncargos(new BigDecimal("10000.00"));
        resumo.setTotalPagamentos(new BigDecimal("60000.00"));
        resumo.setTotalDescontos(new BigDecimal("10000.00"));
        resumo.setTotalLiquido(new BigDecimal("50000.00"));
        resumo.setCompetenciaInicio(COMPETENCIA_INICIO);
        resumo.setCompetenciaFim(COMPETENCIA_FIM);
        resumo.setDataImportacao(DATA_IMPORTACAO);
        resumo.setDecimoTerceiro(false);
        resumo.setAtivo(true);
        return resumo;
    }

    private static FolhaLinhaSnapshot linha(Long funcionarioId, Long centroId, String tipo, String valor) {
        short ob = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        short ol = "DESCONTO".equals(tipo) ? (short) -1 : ("PROVENTO".equals(tipo) ? (short) 1 : (short) 0);
        short oc = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        return new FolhaLinhaSnapshot(
            funcionarioId, "Func " + funcionarioId, centroId, "CC", 1L, "LN", 1L, "Cargo",
            1L, "001", "Rubrica", tipo, new BigDecimal(valor), ob, ol, oc, OrigemLinha.FOLHA_ADP, null);
    }
}
