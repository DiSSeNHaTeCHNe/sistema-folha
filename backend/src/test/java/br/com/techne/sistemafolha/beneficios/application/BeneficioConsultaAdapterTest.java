package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficio;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioCcTipoProjection;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioFuncionarioValorProjection;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalTipoResumoProjection;
import br.com.techne.sistemafolha.beneficios.port.BeneficioCcTipoSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioFuncionarioValorSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficioConsultaAdapterTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private BeneficioMensalRepository beneficioMensalRepository;

    @InjectMocks
    private BeneficioConsultaAdapter adapter;

    @Test
    void somarValorPorFuncionarioECompetencia_somaParcialPorFuncionario() {
        Funcionario funcionario = funcionario(1L);
        BeneficioMensal vr = lancamento(funcionario, new BigDecimal("500.00"));
        BeneficioMensal vt = lancamento(funcionario, new BigDecimal("200.00"));

        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(vr, vt));

        BigDecimal total = adapter.somarValorPorFuncionarioECompetencia(
            1L, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(new BigDecimal("700.00"), total);
    }

    @Test
    void somarValorPorFuncionarioECompetencia_semLancamentos_retornaZero() {
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                2L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of());

        BigDecimal total = adapter.somarValorPorFuncionarioECompetencia(
            2L, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void existeDadosMensaisNaCompetencia_semDados_retornaFalse() {
        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(false);

        assertFalse(adapter.existeDadosMensaisNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM));
    }

    @Test
    void contarLancamentosAtivosNaCompetencia_retornaContagemDashboard() {
        Funcionario funcionario = funcionario(3L);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                lancamento(funcionario, new BigDecimal("100.00")),
                lancamento(funcionario, new BigDecimal("150.00")),
                lancamento(funcionario, new BigDecimal("75.00"))));

        long count = adapter.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(3L, count);
    }

    @Test
    void somarValorPorFuncionarioECompetencia_funcionarioIdNulo_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            adapter.somarValorPorFuncionarioECompetencia(null, COMPETENCIA_INICIO, COMPETENCIA_FIM));
    }

    @Test
    void contarLancamentosPorFuncionarioECompetencia_competenciaNula_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            adapter.contarLancamentosPorFuncionarioECompetencia(1L, null, COMPETENCIA_FIM));
    }

    @Test
    void contarLancamentosAtivosNaCompetenciaPorCentros_linhaCcDiferenteDoFuncionarioAtual_fcc16() {
        when(beneficioMensalRepository.countByCompetenciaECentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
            .thenReturn(1L);
        when(beneficioMensalRepository.countByCompetenciaECentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(200L)))
            .thenReturn(0L);

        long gestorA = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
        assertEquals(1L, gestorA);

        long gestorB = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(200L));
        assertEquals(0L, gestorB);

        verify(beneficioMensalRepository).countByCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
        verify(beneficioMensalRepository).countByCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(200L));
        verify(beneficioMensalRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(COMPETENCIA_INICIO, COMPETENCIA_FIM);
    }

    @Test
    void contarLancamentosAtivosNaCompetenciaPorCentros_usaCountSqlNaoFullFetch_fcc23() {
        when(beneficioMensalRepository.countByCompetenciaECentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
            .thenReturn(2L);

        long count = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));

        assertEquals(2L, count);
        verify(beneficioMensalRepository).countByCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
        verify(beneficioMensalRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void contarLancamentosAtivosNaCompetenciaPorCentros_retornaSubset() {
        when(beneficioMensalRepository.countByCompetenciaECentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
            .thenReturn(2L);

        long count = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));

        assertEquals(2L, count);
        verify(beneficioMensalRepository).countByCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
        verify(beneficioMensalRepository, never())
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void contarLancamentosAtivosNaCompetenciaPorCentros_setVazio_retornaZero() {
        long count = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of());

        assertEquals(0L, count);
    }

    @Test
    void contarLancamentosAtivosNaCompetencia_regressaoUnscoped() {
        Funcionario funcionario = funcionario(3L);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                lancamento(funcionario, new BigDecimal("100.00")),
                lancamento(funcionario, new BigDecimal("150.00")),
                lancamento(funcionario, new BigDecimal("75.00"))));

        long count = adapter.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(3L, count);
    }

    @Test
    void somarValorPorFuncionariosECompetencia_batchRetornaMapaPorFuncionario() {
        when(beneficioMensalRepository.sumValorPorFuncionariosECompetencia(
                Set.of(1L, 2L), COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                new Object[] {1L, new BigDecimal("700.00")},
                new Object[] {2L, new BigDecimal("300.00")}));

        Map<Long, BigDecimal> totais = adapter.somarValorPorFuncionariosECompetencia(
            Set.of(1L, 2L), COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(new BigDecimal("700.00"), totais.get(1L));
        assertEquals(new BigDecimal("300.00"), totais.get(2L));
        verify(beneficioMensalRepository).sumValorPorFuncionariosECompetencia(
            Set.of(1L, 2L), COMPETENCIA_INICIO, COMPETENCIA_FIM);
    }

    @Test
    void somarValorPorFuncionariosECompetencia_setVazio_retornaMapaVazio() {
        Map<Long, BigDecimal> totais = adapter.somarValorPorFuncionariosECompetencia(
            Set.of(), COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(totais.isEmpty());
    }

    @Test
    void somarValorPorCompetenciaECentros_filtraCentrosNaQuery() {
        when(beneficioMensalRepository.sumValorPorCompetenciaECentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
            .thenReturn(new BigDecimal("1250.00"));

        BigDecimal total = adapter.somarValorPorCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));

        assertEquals(new BigDecimal("1250.00"), total);
        verify(beneficioMensalRepository).sumValorPorCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
    }

    @Test
    void somarValorPorCompetenciaECentros_centrosNull_usaConsultaGlobal() {
        when(beneficioMensalRepository.sumValorPorCompetenciaGlobal(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(new BigDecimal("3200.00"));

        BigDecimal total = adapter.somarValorPorCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, null);

        assertEquals(new BigDecimal("3200.00"), total);
        verify(beneficioMensalRepository).sumValorPorCompetenciaGlobal(
            COMPETENCIA_INICIO, COMPETENCIA_FIM);
    }

    @Test
    void somarValorPorCompetenciaECentros_centrosVazios_retornaZero() {
        BigDecimal total = adapter.somarValorPorCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of());

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void somarValorPorCompetenciaECentros_competenciaNula_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            adapter.somarValorPorCompetenciaECentros(null, COMPETENCIA_FIM, Set.of(100L)));
    }

    @Test
    void somarValorPorFuncionarioECompetencia_ignoraValorNull() {
        BeneficioMensal comValor = lancamento(funcionario(1L), new BigDecimal("100.00"));
        BeneficioMensal semValor = lancamento(funcionario(1L), null);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(comValor, semValor));

        assertEquals(new BigDecimal("100.00"),
            adapter.somarValorPorFuncionarioECompetencia(1L, COMPETENCIA_INICIO, COMPETENCIA_FIM));
    }

    @Test
    void contarLancamentosAtivosNaCompetenciaPorCentros_centrosNull_retornaZero() {
        assertEquals(0L, adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, null));
    }

    @Test
    void somarValorPorFuncionariosECompetencia_rowValorNull_trataComoZero() {
        when(beneficioMensalRepository.sumValorPorFuncionariosECompetencia(
                Set.of(1L), COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.<Object[]>of(new Object[]{1L, null}));

        Map<Long, BigDecimal> result = adapter.somarValorPorFuncionariosECompetencia(
            Set.of(1L), COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(BigDecimal.ZERO, result.get(1L));
    }

    @Test
    void findLinhasPorFuncionarioECompetencia_mapeiaSnapshots() {
        TipoBeneficio tipo = new TipoBeneficio();
        tipo.setCodigo("VR");
        tipo.setDescricao("Vale Refeição");
        BeneficioMensal lanc = lancamento(funcionario(1L), new BigDecimal("50.00"));
        lanc.setId(10L);
        lanc.setTipoBeneficio(tipo);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(lanc));

        assertEquals(1, adapter.findLinhasPorFuncionarioECompetencia(
            1L, COMPETENCIA_INICIO, COMPETENCIA_FIM).size());
    }

    @Test
    void somarValorPorCompetenciaECentros_totalNull_retornaZero() {
        when(beneficioMensalRepository.sumValorPorCompetenciaECentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
            .thenReturn(null);

        assertEquals(BigDecimal.ZERO, adapter.somarValorPorCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)));
    }

    @Test
    void validarCompetencia_fimNull_lancaIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
            adapter.existeDadosMensaisNaCompetencia(COMPETENCIA_INICIO, null));
    }

    @Test
    void somarValorPorFuncionariosECompetencia_idsNull_retornaVazio() {
        assertTrue(adapter.somarValorPorFuncionariosECompetencia(
            null, COMPETENCIA_INICIO, COMPETENCIA_FIM).isEmpty());
    }

    @Test
    void somarValorPorCompetenciaECentros_centrosNull_retornaZero() {
        assertEquals(BigDecimal.ZERO, adapter.somarValorPorCompetenciaECentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, null));
    }

    @Test
    void resumoPorTipo_global_retornaSnapshotsOrdenadosPorCodigo() {
        when(beneficioMensalRepository.resumoPorTipoGlobal(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                tipoResumoProjection(1L, "4000", "VR", new BigDecimal("1000"), 5L),
                tipoResumoProjection(2L, "5322", "VT", new BigDecimal("500"), 2L)));

        List<BeneficioTipoResumoSnapshot> result = adapter.resumoPorTipo(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, null);

        assertEquals(2, result.size());
        assertEquals("4000", result.get(0).codigo());
        assertEquals(new BigDecimal("1000"), result.get(0).total());
        assertEquals(5L, result.get(0).qtdLancamentos());
        verify(beneficioMensalRepository).resumoPorTipoGlobal(COMPETENCIA_INICIO, COMPETENCIA_FIM);
    }

    @Test
    void resumoPorTipo_centrosVazios_retornaVazio() {
        assertTrue(adapter.resumoPorTipo(COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of()).isEmpty());
        verify(beneficioMensalRepository, never()).resumoPorTipoGlobal(any(), any());
    }

    @Test
    void resumoPorTipo_scoped_delegaQueryComCentros() {
        when(beneficioMensalRepository.resumoPorTipoCentros(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L)))
            .thenReturn(List.of(tipoResumoProjection(1L, "4000", "VR", new BigDecimal("300"), 1L)));

        List<BeneficioTipoResumoSnapshot> result = adapter.resumoPorTipo(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));

        assertEquals(1, result.size());
        verify(beneficioMensalRepository).resumoPorTipoCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
    }

    @Test
    void topFuncionariosPorTipo_ordenadoValorDesc_limitaDez() {
        when(beneficioMensalRepository.topFuncionariosPorTipoGlobal(1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                funcionarioProjection(10L, "Ana", new BigDecimal("900")),
                funcionarioProjection(11L, "Bob", new BigDecimal("800")),
                funcionarioProjection(12L, "Cal", new BigDecimal("700"))));

        List<BeneficioFuncionarioValorSnapshot> result = adapter.topFuncionariosPorTipo(
            1L, COMPETENCIA_INICIO, COMPETENCIA_FIM, null, 2);

        assertEquals(3, result.size());
        assertEquals("Ana", result.get(0).funcionarioNome());
        assertEquals("CC01", result.get(0).centroCustoCodigo());
        assertTrue(result.get(2).funcionarioNome().startsWith("Outros (1 funcion"));
    }

    @Test
    void topFuncionariosPorTipo_centrosVazios_retornaVazio() {
        assertTrue(adapter.topFuncionariosPorTipo(
            1L, COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(), 10).isEmpty());
    }

    @Test
    void matrizCentroCustoPorTipo_top5x5_filtraMatriz() {
        when(beneficioMensalRepository.matrizCcTipoGlobal(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                ccTipoProjection(1L, "CC A", 10L, "4000", "VR", new BigDecimal("1000")),
                ccTipoProjection(1L, "CC A", 11L, "5322", "VT", new BigDecimal("200")),
                ccTipoProjection(2L, "CC B", 10L, "4000", "VR", new BigDecimal("800")),
                ccTipoProjection(3L, "CC C", 12L, "6000", "PS", new BigDecimal("50"))));

        List<BeneficioCcTipoSnapshot> result = adapter.matrizCentroCustoPorTipo(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, null, 2, 2);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(s -> s.centroCustoId() <= 2 || s.total().compareTo(new BigDecimal("50")) >= 0));
    }

    @Test
    void matrizCentroCustoPorTipo_centrosVazios_retornaVazio() {
        assertTrue(adapter.matrizCentroCustoPorTipo(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(), 5, 5).isEmpty());
    }

    private BeneficioMensalTipoResumoProjection tipoResumoProjection(
            Long tipoId, String codigo, String descricao, BigDecimal total, Long qtd) {
        return new BeneficioMensalTipoResumoProjection() {
            @Override public Long getTipoBeneficioId() { return tipoId; }
            @Override public String getCodigo() { return codigo; }
            @Override public String getDescricao() { return descricao; }
            @Override public BigDecimal getTotal() { return total; }
            @Override public Long getQtdLancamentos() { return qtd; }
        };
    }

    private BeneficioFuncionarioValorProjection funcionarioProjection(
            Long id, String nome, BigDecimal valor) {
        return funcionarioProjection(id, nome, valor, "CC01", "Centro Admin");
    }

    private BeneficioFuncionarioValorProjection funcionarioProjection(
            Long id, String nome, BigDecimal valor, String ccCodigo, String ccDescricao) {
        return new BeneficioFuncionarioValorProjection() {
            @Override public Long getFuncionarioId() { return id; }
            @Override public String getFuncionarioNome() { return nome; }
            @Override public BigDecimal getValor() { return valor; }
            @Override public String getCentroCustoCodigo() { return ccCodigo; }
            @Override public String getCentroCustoDescricao() { return ccDescricao; }
        };
    }

    private BeneficioCcTipoProjection ccTipoProjection(
            Long ccId, String ccDesc, Long tipoId, String tipoCodigo, String tipoDesc, BigDecimal total) {
        return new BeneficioCcTipoProjection() {
            @Override public Long getCentroCustoId() { return ccId; }
            @Override public String getCentroCustoDescricao() { return ccDesc; }
            @Override public Long getTipoBeneficioId() { return tipoId; }
            @Override public String getTipoCodigo() { return tipoCodigo; }
            @Override public String getTipoDescricao() { return tipoDesc; }
            @Override public BigDecimal getTotal() { return total; }
        };
    }

    private Funcionario funcionario(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        return funcionario;
    }

    private Funcionario funcionarioComCentro(Long id, Long centroCustoId) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(centroCustoId);

        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setCentroCusto(centroCusto);
        return funcionario;
    }

    private BeneficioMensal lancamento(Funcionario funcionario, BigDecimal valor) {
        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setFuncionario(funcionario);
        beneficio.setValor(valor);
        beneficio.setCompetenciaInicio(COMPETENCIA_INICIO);
        beneficio.setCompetenciaFim(COMPETENCIA_FIM);
        beneficio.setAtivo(true);
        return beneficio;
    }
}
