package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaTotalizacaoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @Mock
    private EncargosRateioService encargosRateioService;

    @InjectMocks
    private FolhaTotalizacaoService folhaTotalizacaoService;

    @Test
    void calcularTotaisPorFuncionario_quandoPortRetornaLancamentos_somaBeneficiosMensais() {
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(
                Set.of(1L), COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(Map.of(1L, new BigDecimal("700.00")));
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(2);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService.calcularTotaisPorFuncionario(
            List.of(linhaSnapshot(1L, "Ana Silva", (short) 1, (short) 1, (short) 1, "8000.00")),
            contextoAcessoTotal(),
            BigDecimal.ZERO,
            COMPETENCIA_INICIO,
            COMPETENCIA_FIM);

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("8000.00"), total.salCustoFolha());
        assertEquals(new BigDecimal("700.00"), total.salCustoBeneficios());
        assertEquals(BigDecimal.ZERO.setScale(2), total.encargosRateados());
        assertEquals(new BigDecimal("8700.00"), total.custoEmpresa());
        assertEquals(2, total.totalBeneficios());

        verify(beneficioConsultaPort).somarValorPorFuncionariosECompetencia(
            eq(Set.of(1L)), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM));
    }

    @Test
    void calcularTotaisPorFuncionario_quandoPortRetornaZero_custoBeneficiosZero() {
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of());
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                2L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService.calcularTotaisPorFuncionario(
            List.of(linhaSnapshot(2L, "Bruno Costa", (short) 1, (short) 1, (short) 1, "6000.00")),
            contextoAcessoTotal(),
            BigDecimal.ZERO,
            COMPETENCIA_INICIO,
            COMPETENCIA_FIM);

        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("6000.00"), total.salCustoFolha());
        assertEquals(BigDecimal.ZERO.setScale(2), total.salCustoBeneficios());
        assertEquals(new BigDecimal("6000.00"), total.custoEmpresa());
    }

    @Test
    void calcularTotaisPorFuncionario_listaVazia_retornaVazio() {
        assertEquals(List.of(), folhaTotalizacaoService.calcularTotaisPorFuncionario(
            List.of(), contextoAcessoTotal(), BigDecimal.ZERO, COMPETENCIA_INICIO, COMPETENCIA_FIM));
    }

    @Test
    void calcularTotaisPorFuncionario_proventoEDesconto_descontoImpactaSomenteLiquido() {
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of());
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                4L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService.calcularTotaisPorFuncionario(
            List.of(
                linhaSnapshot(4L, "Diana", (short) 1, (short) 1, (short) 1, "10000.00"),
                linhaSnapshot(4L, "Diana", (short) 0, (short) -1, (short) 0, "1500.00")),
            contextoScoped(),
            BigDecimal.ZERO,
            COMPETENCIA_INICIO,
            COMPETENCIA_FIM);

        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("10000.00"), total.salBruto());
        assertEquals(new BigDecimal("8500.00"), total.salLiquido());
        assertEquals(new BigDecimal("10000.00"), total.salCustoFolha());
        assertEquals(BigDecimal.ZERO.setScale(2), total.encargosRateados());
    }

    @Test
    void calcularTotaisPorFuncionario_global_rateiaEncargosPorFuncionario() {
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of());
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(any(), any(), any()))
            .thenReturn(0);
        when(encargosRateioService.ratearPorFuncionario(any(), eq(new BigDecimal("1000.00"))))
            .thenReturn(Map.of(1L, new BigDecimal("800.00"), 2L, new BigDecimal("200.00")));

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService.calcularTotaisPorFuncionario(
            List.of(
                linhaSnapshot(1L, "A", (short) 1, (short) 1, (short) 1, "8000.00"),
                linhaSnapshot(2L, "B", (short) 1, (short) 1, (short) 1, "2000.00")),
            contextoAcessoTotal(),
            new BigDecimal("1000.00"),
            COMPETENCIA_INICIO,
            COMPETENCIA_FIM);

        FolhaTotaisFuncionarioDTO func1 = resultado.stream().filter(t -> t.funcionarioId().equals(1L)).findFirst().orElseThrow();
        FolhaTotaisFuncionarioDTO func2 = resultado.stream().filter(t -> t.funcionarioId().equals(2L)).findFirst().orElseThrow();
        assertEquals(new BigDecimal("800.00"), func1.encargosRateados());
        assertEquals(new BigDecimal("200.00"), func2.encargosRateados());
        assertEquals(new BigDecimal("8800.00"), func1.custoEmpresa());
        assertEquals(new BigDecimal("2200.00"), func2.custoEmpresa());
    }

    @Test
    void calcularTotaisPorFuncionario_arredondaHalfUpDuasCasas() {
        when(beneficioConsultaPort.somarValorPorFuncionariosECompetencia(any(), any(), any()))
            .thenReturn(Map.of());
        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                6L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService.calcularTotaisPorFuncionario(
            List.of(linhaSnapshot(6L, "Fabio", (short) 1, (short) 1, (short) 1, "10.005")),
            contextoScoped(),
            BigDecimal.ZERO,
            COMPETENCIA_INICIO,
            COMPETENCIA_FIM);

        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("10.01"), total.salBruto());
        assertEquals(new BigDecimal("10.01"), total.salLiquido());
        assertEquals(new BigDecimal("10.01"), total.salCustoFolha());
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, null, null, null);
    }

    private AccessContextDTO contextoScoped() {
        return new AccessContextDTO(true, true, false, Set.of(10L), null, 2L, "TI", 1);
    }

    private FolhaLinhaSnapshot linhaSnapshot(
            Long funcionarioId, String nome, short ob, short ol, short oc, String valor) {
        return new FolhaLinhaSnapshot(
            funcionarioId, nome, 10L, "TI", 1L, "LN", 1L, "Cargo",
            1L, "001", "Rubrica", "PROVENTO", new BigDecimal(valor), ob, ol, oc, OrigemLinha.FOLHA_ADP, null);
    }
}
