package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
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
        CentroCusto ccSnapshot = new CentroCusto();
        ccSnapshot.setId(100L);
        CentroCusto ccAtual = new CentroCusto();
        ccAtual.setId(200L);

        Funcionario funcionario = funcionarioComCentro(1L, 200L);
        funcionario.setCentroCusto(ccAtual);

        BeneficioMensal beneficio = lancamento(funcionario, new BigDecimal("100.00"));
        beneficio.setCentroCusto(ccSnapshot);

        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(beneficio));

        long gestorA = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));
        assertEquals(1L, gestorA);

        long gestorB = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(200L));
        assertEquals(0L, gestorB);
    }

    @Test
    void contarLancamentosAtivosNaCompetenciaPorCentros_retornaSubset() {
        Funcionario fCc1 = funcionarioComCentro(1L, 100L);
        Funcionario fCc2 = funcionarioComCentro(2L, 200L);

        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(
                lancamento(fCc1, new BigDecimal("100.00")),
                lancamento(fCc1, new BigDecimal("150.00")),
                lancamento(fCc2, new BigDecimal("75.00"))));

        long count = adapter.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));

        assertEquals(2L, count);
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
