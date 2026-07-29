package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalCompetenciaResumoDTO;
import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalDTO;
import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalResumoDTO;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficio;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalCompetenciaProjection;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalResumoProjection;
import br.com.techne.sistemafolha.beneficios.infrastructure.TipoBeneficioRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficioMensalServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);
    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;

    @Mock
    private BeneficioMensalRepository beneficioMensalRepository;

    @Mock
    private FuncionarioConsultaPort funcionarioConsultaPort;

    @Mock
    private TipoBeneficioRepository tipoBeneficioRepository;

    @Mock
    private UsuarioLookupPort usuarioLookupPort;

    @Mock
    private OrganogramaAcessoPort organogramaAcessoPort;

    @InjectMocks
    private BeneficioMensalService beneficioMensalService;

    @Test
    void listarPorCompetenciaParaUsuario_acesso_negado_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void listarPorCompetenciaParaUsuario_restrito_sem_centros_retorna_vazio_sem_query_unscoped() {
        // MODACL-01 / MODACL-05: distinto de SEM_FUNCIONARIO — temFuncionario+temNo, centros vazios
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Collections.emptySet()));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
        verify(beneficioMensalRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                any(), any(), any());
    }

    @Test
    void resumoPorCompetenciaParaUsuario_restrito_sem_centros_retorna_vazio_sem_agregacao_unscoped() {
        // MODACL-02 / MODACL-05: distinto de SEM_FUNCIONARIO — temFuncionario+temNo, centros vazios
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Collections.emptySet()));

        List<BeneficioMensalResumoDTO> result = beneficioMensalService.resumoPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository, never()).resumoPorCompetencia(any(), any());
        verify(beneficioMensalRepository, never())
            .resumoPorCompetenciaAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void listarPorCompetenciaParaUsuario_acesso_total_usa_query_unscoped() {
        // MODACL-03
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotal());
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(1, result.size());
        verify(beneficioMensalRepository).findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM);
        verify(beneficioMensalRepository, never())
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        any(), any(), any());
    }

    @Test
    void listarPorCompetenciaParaUsuario_restrito_com_centros_usa_query_In() {
        // MODACL-04
        stubUsuario();
        Set<Long> centros = Set.of(10L, 20L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(centros));
        BeneficioMensal beneficio = beneficioAtivo(2L);
        when(beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(1, result.size());
        verify(beneficioMensalRepository)
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);
        verify(beneficioMensalRepository, never())
                .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void listarCompetenciasParaUsuario_acesso_negado_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        List<BeneficioMensalCompetenciaResumoDTO> result =
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository, never()).competenciasResumo(any(), any());
        verify(beneficioMensalRepository, never())
            .competenciasResumoAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void listarCompetenciasParaUsuario_restrito_sem_centros_retorna_vazio_sem_agregacao_unscoped() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Collections.emptySet()));

        List<BeneficioMensalCompetenciaResumoDTO> result =
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository, never()).competenciasResumo(any(), any());
        verify(beneficioMensalRepository, never())
            .competenciasResumoAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void listarCompetenciasParaUsuario_acesso_total_usa_query_sem_filtro_centro() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotal());
        LocalDate inicioAno = LocalDate.of(2024, 1, 1);
        LocalDate fimAno = LocalDate.of(2024, 12, 31);
        BeneficioMensalCompetenciaProjection projection = competenciaProjection(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, 5L, new BigDecimal("3000.00"), 10L);
        when(beneficioMensalRepository.competenciasResumo(inicioAno, fimAno))
            .thenReturn(List.of(projection));

        List<BeneficioMensalCompetenciaResumoDTO> result =
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).totalFuncionarios());
        assertEquals(new BigDecimal("3000.00"), result.get(0).totalBeneficios());
        assertEquals(10L, result.get(0).qtdLancamentos());
        verify(beneficioMensalRepository).competenciasResumo(inicioAno, fimAno);
        verify(beneficioMensalRepository, never())
            .competenciasResumoAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void listarCompetenciasParaUsuario_restrito_com_centros_usa_query_com_centros() {
        stubUsuario();
        Set<Long> centros = Set.of(10L, 20L);
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(centros));
        LocalDate inicioAno = LocalDate.of(2024, 1, 1);
        LocalDate fimAno = LocalDate.of(2024, 12, 31);
        BeneficioMensalCompetenciaProjection projection = competenciaProjection(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, 2L, new BigDecimal("800.00"), 4L);
        when(beneficioMensalRepository.competenciasResumoAndCentroCustoIds(inicioAno, fimAno, centros))
            .thenReturn(List.of(projection));

        List<BeneficioMensalCompetenciaResumoDTO> result =
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null);

        assertEquals(1, result.size());
        verify(beneficioMensalRepository).competenciasResumoAndCentroCustoIds(inicioAno, fimAno, centros);
        verify(beneficioMensalRepository, never()).competenciasResumo(any(), any());
    }

    @Test
    void listarCompetenciasParaUsuario_filtro_ano_mes_restringe_ao_mes() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoAcessoTotal());
        LocalDate inicioMes = LocalDate.of(2024, 10, 1);
        LocalDate fimMes = LocalDate.of(2024, 10, 31);
        when(beneficioMensalRepository.competenciasResumo(inicioMes, fimMes))
            .thenReturn(Collections.emptyList());

        List<BeneficioMensalCompetenciaResumoDTO> result =
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, 10);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository).competenciasResumo(inicioMes, fimMes);
        verify(beneficioMensalRepository, never())
            .competenciasResumoAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void criarParaUsuario_sem_acesso_ao_centro_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(99L)));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(funcionarioAtivo(1L)));

        BeneficioMensalDTO dto = dtoBase(null, 1L, 2L, new BigDecimal("450.00"));
        Optional<BeneficioMensalDTO> result = beneficioMensalService.criarParaUsuario(LOGIN, dto);

        assertTrue(result.isEmpty());
        verify(beneficioMensalRepository, never()).save(any());
    }

    @Test
    void removerSeAutorizado_beneficio_fora_do_centro_retorna_false() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(99L)));
        when(beneficioMensalRepository.findById(7L)).thenReturn(Optional.of(beneficioAtivo(7L)));

        assertFalse(beneficioMensalService.removerSeAutorizado(LOGIN, 7L));
        verify(beneficioMensalRepository, never()).save(any());
    }

    @Test
    void removerSeAutorizado_com_acesso_executa_soft_delete() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        BeneficioMensal beneficio = beneficioAtivo(7L);
        when(beneficioMensalRepository.findById(7L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        assertTrue(beneficioMensalService.removerSeAutorizado(LOGIN, 7L));
        assertFalse(beneficio.getAtivo());
    }

    @Test
    void listarPorCompetencia_acesso_total_usa_query_sem_filtro_centro() {
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Collections.emptySet());

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        verify(beneficioMensalRepository).findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM);
        verify(beneficioMensalRepository, never())
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        any(), any(), any());
    }

    @Test
    void listarPorCompetenciaParaUsuario_snapshotCcDiferenteDoFuncionarioAtual_filtraPorCcDaLinha_fcc14() {
        // FCC-14: snapshot CC-A (100L), funcionário atual CC-B (200L)
        stubUsuario();

        CentroCusto ccSnapshot = new CentroCusto();
        ccSnapshot.setId(100L);
        ccSnapshot.setDescricao("CC Alpha");
        CentroCusto ccAtual = new CentroCusto();
        ccAtual.setId(200L);
        ccAtual.setDescricao("CC Beta");

        Funcionario funcionario = funcionarioAtivo(99L);
        funcionario.setCentroCusto(ccAtual);

        BeneficioMensal beneficio = beneficioAtivo(3L);
        beneficio.setFuncionario(funcionario);
        beneficio.setCentroCusto(ccSnapshot);

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(100L)));
        when(beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> gestorA = beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);
        assertEquals(1, gestorA.size());
        assertEquals(3L, gestorA.get(0).id());
        assertEquals(100L, gestorA.get(0).centroCustoId());

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(200L)));
        when(beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(200L)))
                .thenReturn(List.of());

        List<BeneficioMensalDTO> gestorB = beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM);
        assertTrue(gestorB.isEmpty());
    }

    @Test
    void listarPorCompetencia_acesso_restrito_usa_query_com_centros() {
        Set<Long> centros = Set.of(10L, 20L);
        BeneficioMensal beneficio = beneficioAtivo(2L);
        when(beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
        verify(beneficioMensalRepository)
                .findByCompetenciaInicioAndCompetenciaFimAndCentroCustoEfetivoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);
        verify(beneficioMensalRepository, never())
                .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void resumoPorCompetencia_acesso_total_usa_resumo_sem_filtro() {
        BeneficioMensalResumoProjection projection = resumoProjection(
                "VALE_REFEICAO", "Vale Refeição - Custo Empresa", new BigDecimal("1500.00"), 3L);
        when(beneficioMensalRepository.resumoPorCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(projection));

        List<BeneficioMensalResumoDTO> result = beneficioMensalService.resumoPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Collections.emptySet());

        assertEquals(1, result.size());
        assertEquals("VALE_REFEICAO", result.get(0).codigo());
        assertEquals(new BigDecimal("1500.00"), result.get(0).total());
        assertEquals(3L, result.get(0).qtdLancamentos());
        verify(beneficioMensalRepository).resumoPorCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM);
        verify(beneficioMensalRepository, never())
                .resumoPorCompetenciaAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void resumoPorCompetencia_acesso_restrito_usa_resumo_com_centros() {
        Set<Long> centros = Set.of(10L);
        BeneficioMensalResumoProjection projection = resumoProjection(
                "SEGUROS", "Seguros - Custo Empresa", new BigDecimal("800.00"), 2L);
        when(beneficioMensalRepository.resumoPorCompetenciaAndCentroCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
                .thenReturn(List.of(projection));

        List<BeneficioMensalResumoDTO> result = beneficioMensalService.resumoPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);

        assertEquals(1, result.size());
        assertEquals("SEGUROS", result.get(0).codigo());
        verify(beneficioMensalRepository).resumoPorCompetenciaAndCentroCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);
        verify(beneficioMensalRepository, never()).resumoPorCompetencia(any(), any());
    }

    @Test
    void listarPorFuncionario_retorna_lancamentos_do_periodo() {
        BeneficioMensal beneficio = beneficioAtivo(5L);
        when(beneficioMensalRepository
                .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                        99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorFuncionario(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals(99L, result.get(0).funcionarioId());
    }

    @Test
    void criar_persisteCentroCustoSnapshot_fcc13() {
        BeneficioMensalDTO dto = dtoBase(null, 1L, 2L, new BigDecimal("450.00"));
        Funcionario funcionario = funcionarioAtivo(1L);
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(funcionario));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.of(tipoAtivo(2L, "VALE_REFEICAO")));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> {
            BeneficioMensal bm = inv.getArgument(0);
            bm.setId(10L);
            return bm;
        });

        beneficioMensalService.criar(dto);

        verify(beneficioMensalRepository).save(org.mockito.ArgumentMatchers.argThat(bm ->
            bm.getCentroCusto() != null && bm.getCentroCusto().getId().equals(10L)));
    }

    @Test
    void removerSeAutorizado_usaCcSnapshotNaoCcAtual_fcc15() {
        stubUsuario();
        CentroCusto ccSnapshot = new CentroCusto();
        ccSnapshot.setId(100L);
        CentroCusto ccAtual = new CentroCusto();
        ccAtual.setId(200L);

        Funcionario funcionario = funcionarioAtivo(99L);
        funcionario.setCentroCusto(ccAtual);

        BeneficioMensal beneficio = beneficioAtivo(7L);
        beneficio.setFuncionario(funcionario);
        beneficio.setCentroCusto(ccSnapshot);

        when(beneficioMensalRepository.findById(7L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(100L)));
        assertTrue(beneficioMensalService.removerSeAutorizado(LOGIN, 7L));

        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(200L)));
        assertFalse(beneficioMensalService.removerSeAutorizado(LOGIN, 7L));
    }

    @Test
    void toDTO_refleteCcDaLinhaComFallback_fcc17() {
        CentroCusto ccSnapshot = new CentroCusto();
        ccSnapshot.setId(100L);
        ccSnapshot.setDescricao("CC Alpha");

        CentroCusto ccAtual = new CentroCusto();
        ccAtual.setId(200L);
        ccAtual.setDescricao("CC Beta");

        Funcionario funcionario = funcionarioAtivo(99L);
        funcionario.setCentroCusto(ccAtual);

        BeneficioMensal beneficio = beneficioAtivo(5L);
        beneficio.setFuncionario(funcionario);
        beneficio.setCentroCusto(ccSnapshot);

        when(beneficioMensalRepository
                .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                        99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorFuncionario(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(100L, result.get(0).centroCustoId());
        assertEquals("CC Alpha", result.get(0).centroCustoDescricao());
    }

    @Test
    void criar_persiste_beneficio_ativo() {
        BeneficioMensalDTO dto = dtoBase(null, 1L, 2L, new BigDecimal("450.00"));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(funcionarioAtivo(1L)));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.of(tipoAtivo(2L, "VALE_REFEICAO")));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> {
            BeneficioMensal bm = inv.getArgument(0);
            bm.setId(10L);
            return bm;
        });

        BeneficioMensalDTO result = beneficioMensalService.criar(dto);

        assertEquals(10L, result.id());
        assertEquals("VALE_REFEICAO", result.tipoBeneficioCodigo());
        assertEquals(new BigDecimal("450.00"), result.valor());
        verify(beneficioMensalRepository).save(any(BeneficioMensal.class));
    }

    @Test
    void remover_desativa_beneficio() {
        BeneficioMensal beneficio = beneficioAtivo(7L);
        when(beneficioMensalRepository.findById(7L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        beneficioMensalService.remover(7L);

        assertFalse(beneficio.getAtivo());
        verify(beneficioMensalRepository).save(beneficio);
    }

    @Test
    void remover_lanca_excecao_quando_nao_encontrado() {
        when(beneficioMensalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BeneficioMensalNotFoundException.class, () -> beneficioMensalService.remover(999L));
        verify(beneficioMensalRepository, never()).save(any());
    }

    private BeneficioMensalDTO dtoBase(Long id, Long funcionarioId, Long tipoBeneficioId, BigDecimal valor) {
        return new BeneficioMensalDTO(
                id,
                funcionarioId,
                null,
                tipoBeneficioId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                valor,
                COMPETENCIA_INICIO,
                COMPETENCIA_FIM,
                null
        );
    }

    private BeneficioMensal beneficioAtivo(Long id) {
        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setId(id);
        beneficio.setFuncionario(funcionarioAtivo(99L));
        beneficio.setTipoBeneficio(tipoAtivo(2L, "VALE_REFEICAO"));
        beneficio.setValor(new BigDecimal("450.00"));
        beneficio.setCompetenciaInicio(COMPETENCIA_INICIO);
        beneficio.setCompetenciaFim(COMPETENCIA_FIM);
        beneficio.setAtivo(true);
        return beneficio;
    }

    private Funcionario funcionarioAtivo(Long id) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(10L);
        centroCusto.setDescricao("TI");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("João Silva");
        funcionario.setCentroCusto(centroCusto);
        funcionario.setAtivo(true);
        return funcionario;
    }

    private TipoBeneficio tipoAtivo(Long id, String codigo) {
        TipoBeneficio tipo = new TipoBeneficio();
        tipo.setId(id);
        tipo.setCodigo(codigo);
        tipo.setDescricao("Vale Refeição - Custo Empresa");
        tipo.setAtivo(true);
        return tipo;
    }

    private BeneficioMensalResumoProjection resumoProjection(
            String codigo, String descricao, BigDecimal total, Long qtdLancamentos) {
        return new BeneficioMensalResumoProjection() {
            @Override
            public String getCodigo() {
                return codigo;
            }

            @Override
            public String getDescricao() {
                return descricao;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }

            @Override
            public Long getQtdLancamentos() {
                return qtdLancamentos;
            }
        };
    }

    private BeneficioMensalCompetenciaProjection competenciaProjection(
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            Long totalFuncionarios,
            BigDecimal totalBeneficios,
            Long qtdLancamentos) {
        return new BeneficioMensalCompetenciaProjection() {
            @Override
            public LocalDate getCompetenciaInicio() {
                return competenciaInicio;
            }

            @Override
            public LocalDate getCompetenciaFim() {
                return competenciaFim;
            }

            @Override
            public Long getTotalFuncionarios() {
                return totalFuncionarios;
            }

            @Override
            public BigDecimal getTotalBeneficios() {
                return totalBeneficios;
            }

            @Override
            public Long getQtdLancamentos() {
                return qtdLancamentos;
            }
        };
    }

    private void stubUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setAtivo(true);
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private AccessContextDTO contextoNegado(MotivoNegacaoAcesso motivo) {
        return new AccessContextDTO(false, false, false, Set.of(), motivo, null, null, null);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Collections.emptySet(), null, null, null, null);
    }
}
