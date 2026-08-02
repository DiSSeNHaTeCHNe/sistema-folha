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
    void criarParaUsuario_com_acesso_persiste_beneficio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(funcionarioAtivo(1L)));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.of(tipoAtivo(2L, "VALE_REFEICAO")));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> {
            BeneficioMensal bm = inv.getArgument(0);
            bm.setId(10L);
            return bm;
        });

        BeneficioMensalDTO dto = dtoBase(null, 1L, 2L, new BigDecimal("450.00"));
        Optional<BeneficioMensalDTO> result = beneficioMensalService.criarParaUsuario(LOGIN, dto);

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().id());
        verify(beneficioMensalRepository).save(any(BeneficioMensal.class));
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

    @Test
    void resumoPorCompetenciaParaUsuario_acesso_negado_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA));

        assertTrue(beneficioMensalService.resumoPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void resumoPorCompetenciaParaUsuario_centrosNull_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertTrue(beneficioMensalService.resumoPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void listarPorFuncionarioParaUsuario_acesso_negado_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoNegado(MotivoNegacaoAcesso.SEM_FUNCIONARIO));

        assertTrue(beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void listarPorFuncionarioParaUsuario_semOrganograma_filtra_tudo() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, false, false, Set.of(10L), null, 2L, "TI", 1));

        assertTrue(beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
        verify(beneficioMensalRepository, never())
            .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any(), any());
    }

    @Test
    void listarPorFuncionarioParaUsuario_scoped_filtraPorCentro() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(1, result.size());
    }

    @Test
    void listarCompetenciasParaUsuario_anoInvalido_lancaExcecao() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        assertThrows(IllegalArgumentException.class, () ->
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 1999, null));
    }

    @Test
    void listarCompetenciasParaUsuario_centrosNull_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, null, null, 2L, "TI", 1));

        assertTrue(beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null).isEmpty());
    }

    @Test
    void listarCompetencias_projectionNulls_usaDefaults() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(beneficioMensalRepository.competenciasResumo(any(), any()))
            .thenReturn(List.of(competenciaProjection(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, null, null, null)));

        List<BeneficioMensalCompetenciaResumoDTO> result = beneficioMensalService.listarCompetenciasParaUsuario(
            LOGIN, 2024, 10);

        assertEquals(0L, result.get(0).totalFuncionarios());
        assertEquals(BigDecimal.ZERO, result.get(0).totalBeneficios());
        assertEquals(0L, result.get(0).qtdLancamentos());
    }

    @Test
    void listarPorCompetencia_toDtoCamposNull_mapeiaNulls() {
        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setId(1L);
        beneficio.setValor(new BigDecimal("100.00"));
        beneficio.setCompetenciaInicio(COMPETENCIA_INICIO);
        beneficio.setCompetenciaFim(COMPETENCIA_FIM);
        beneficio.setAtivo(true);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficio));

        BeneficioMensalDTO dto = beneficioMensalService.listarPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Collections.emptySet()).get(0);

        assertEquals(null, dto.funcionarioId());
        assertEquals(null, dto.tipoBeneficioId());
        assertEquals(null, dto.centroCustoId());
        assertEquals(null, dto.linhaNegocioId());
    }

    @Test
    void criarParaUsuario_funcionarioSemCentro_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        Funcionario func = funcionarioAtivo(99L);
        func.setCentroCusto(null);
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(99L)).thenReturn(Optional.of(func));

        assertTrue(beneficioMensalService.criarParaUsuario(
            LOGIN, dtoBase(null, 99L, 2L, new BigDecimal("100.00"))).isEmpty());
    }

    @Test
    void listarPorFuncionarioParaUsuario_dtoSemCentroCusto_exclui() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        beneficio.setCentroCusto(null);
        beneficio.getFuncionario().setCentroCusto(null);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficio));

        assertTrue(beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void resumoPorCompetenciaParaUsuario_centrosVazios_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        assertTrue(beneficioMensalService.resumoPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void listarCompetenciasParaUsuario_ano2101_lancaExcecao() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        assertThrows(IllegalArgumentException.class, () ->
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2101, null));
    }

    @Test
    void listarCompetencias_usuarioNaoEncontrado_lancaRuntimeException() {
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
            beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null));
    }

    @Test
    void criar_funcionarioNaoEncontrado_lancaExcecao() {
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());
        assertThrows(br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException.class, () ->
            beneficioMensalService.criar(dtoBase(null, 99L, 2L, new BigDecimal("100.00"))));
    }

    @Test
    void criar_tipoBeneficioNaoEncontrado_lancaExcecao() {
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(99L)).thenReturn(Optional.of(funcionarioAtivo(99L)));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException.class, () ->
            beneficioMensalService.criar(dtoBase(null, 99L, 2L, new BigDecimal("100.00"))));
    }

    @Test
    void criar_tipoBeneficioInativo_lancaExcecao() {
        TipoBeneficio inativo = tipoAtivo(2L, "VR");
        inativo.setAtivo(false);
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(99L)).thenReturn(Optional.of(funcionarioAtivo(99L)));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.of(inativo));
        assertThrows(br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException.class, () ->
            beneficioMensalService.criar(dtoBase(null, 99L, 2L, new BigDecimal("100.00"))));
    }

    @Test
    void toDTO_comLinhaNegocioECargo_mapeiaCampos() {
        br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio ln =
            new br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio();
        ln.setId(5L);
        ln.setDescricao("TI");
        CentroCusto cc = new CentroCusto();
        cc.setId(10L);
        cc.setDescricao("CC TI");
        cc.setLinhaNegocio(ln);
        br.com.techne.sistemafolha.cadastros.domain.Cargo cargo =
            new br.com.techne.sistemafolha.cadastros.domain.Cargo();
        cargo.setDescricao("Analista");
        Funcionario func = funcionarioAtivo(99L);
        func.setCentroCusto(cc);
        func.setCargo(cargo);
        BeneficioMensal beneficio = beneficioAtivo(1L);
        beneficio.setFuncionario(func);
        beneficio.setCentroCusto(cc);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficio));

        BeneficioMensalDTO dto = beneficioMensalService.listarPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Collections.emptySet()).get(0);

        assertEquals(5L, dto.linhaNegocioId());
        assertEquals("Analista", dto.cargoDescricao());
    }

    @Test
    void removerSeAutorizado_entitySemCentroCusto_retornaFalse() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        beneficio.setCentroCusto(null);
        beneficio.getFuncionario().setCentroCusto(null);
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficio));

        assertFalse(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void removerSeAutorizado_semFuncionarioVinculado_retornaFalse() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(false, true, false, Set.of(10L), null, 2L, "TI", 1));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficio));

        assertFalse(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void listarPorCompetenciaParaUsuario_restrito_centrosVazios_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        assertTrue(beneficioMensalService.listarPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void listarCompetenciasParaUsuario_anoOmitido_usaAnoCorrente() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(beneficioMensalRepository.competenciasResumo(any(), any())).thenReturn(List.of());

        assertTrue(beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, null, null).isEmpty());
    }

    @Test
    void removerSeAutorizado_entityComCentroNoEscopo_retornaTrue() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        assertTrue(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void listarPorFuncionarioParaUsuario_dtoCentroForaDoEscopo_exclui() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(99L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficio));

        assertTrue(beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void removerSeAutorizado_semOrganograma_retornaFalse() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, false, false, Set.of(10L), null, 2L, "TI", 1));
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficioAtivo(1L)));

        assertFalse(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void removerSeAutorizado_acessoTotal_retornaTrue() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        assertTrue(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void removerSeAutorizado_entityCentroDaLinhaNoEscopo_retornaTrue() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(20L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        CentroCusto ccLinha = new CentroCusto();
        ccLinha.setId(20L);
        beneficio.setCentroCusto(ccLinha);
        beneficio.getFuncionario().getCentroCusto().setId(10L);
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        assertTrue(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void criarParaUsuario_semOrganograma_retornaVazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, false, false, Set.of(10L), null, 2L, "TI", 1));
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(99L)).thenReturn(Optional.of(funcionarioAtivo(99L)));

        assertTrue(beneficioMensalService.criarParaUsuario(
            LOGIN, dtoBase(null, 99L, 2L, new BigDecimal("100.00"))).isEmpty());
    }

    @Test
    void listarPorFuncionarioParaUsuario_dtoCentroNoEscopo_inclui() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficioAtivo(1L)));

        assertEquals(1, beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM).size());
    }

    @Test
    void listarCompetenciasParaUsuario_restrito_centrosVazios_retorna_vazio() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(new AccessContextDTO(true, true, false, Collections.emptySet(), null, 2L, "TI", 1));

        assertTrue(beneficioMensalService.listarCompetenciasParaUsuario(LOGIN, 2024, null).isEmpty());
    }

    @Test
    void resumoPorCompetenciaParaUsuario_restrito_comCentros_delega() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        when(beneficioMensalRepository.resumoPorCompetenciaAndCentroCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(10L)))
            .thenReturn(List.of(resumoProjection("VR", "Vale", new BigDecimal("100.00"), 1L)));

        assertEquals(1, beneficioMensalService.resumoPorCompetenciaParaUsuario(
            LOGIN, COMPETENCIA_INICIO, COMPETENCIA_FIM).size());
    }

    @Test
    void criarParaUsuario_acessoTotal_persiste() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(funcionarioConsultaPort.findByIdAndAtivoTrue(99L)).thenReturn(Optional.of(funcionarioAtivo(99L)));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.of(tipoAtivo(2L, "VR")));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> {
            BeneficioMensal b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        assertTrue(beneficioMensalService.criarParaUsuario(
            LOGIN, dtoBase(null, 99L, 2L, new BigDecimal("100.00"))).isPresent());
    }

    @Test
    void removerSeAutorizado_entitySemCentroNaLinha_usaCentroFuncionario() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID))
            .thenReturn(contextoRestrito(Set.of(10L)));
        BeneficioMensal beneficio = beneficioAtivo(1L);
        beneficio.setCentroCusto(null);
        when(beneficioMensalRepository.findById(1L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        assertTrue(beneficioMensalService.removerSeAutorizado(LOGIN, 1L));
    }

    @Test
    void listarPorFuncionarioParaUsuario_acessoTotal_incluiTodos() {
        stubUsuario();
        when(organogramaAcessoPort.obterContextoAcesso(USUARIO_ID)).thenReturn(contextoAcessoTotal());
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficioAtivo(1L)));

        assertEquals(1, beneficioMensalService.listarPorFuncionarioParaUsuario(
            LOGIN, 99L, COMPETENCIA_INICIO, COMPETENCIA_FIM).size());
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
