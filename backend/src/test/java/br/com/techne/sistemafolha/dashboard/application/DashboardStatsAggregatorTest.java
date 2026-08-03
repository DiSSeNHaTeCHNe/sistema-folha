package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaTotalizacaoPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatsAggregatorTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 6, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 6, 30);

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private FolhaTotalizacaoPort folhaTotalizacaoPort;

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    private DashboardStatsAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = new DashboardStatsAggregator(folhaConsultaPort, folhaTotalizacaoPort, beneficioConsultaPort);
    }

    @Test
    void aggregateForCompetencia_acessoTotal_retornaTotaisEsperados() {
        AccessContextDTO contexto = new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
        FolhaLinhaSnapshot linha = linha(100L, 10L, "Centro A", 1L, "LN", 200L, "Dev",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("8000.00"));

        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null))
            .thenReturn(List.of(linha));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(any(), any(), any(), eq(contexto)))
            .thenReturn(new BigDecimal("9000.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(3L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of());

        DashboardStatsDTO stats = aggregator.aggregateForCompetencia(
            contexto, null, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("9000.00"), stats.custoMensalFolha());
        assertEquals(3L, stats.totalBeneficiosAtivos());
        assertEquals(new BigDecimal("8000.00"), stats.totalProventos());
    }

    @Test
    void aggregateForCompetencia_scoped_filtraPorCentros() {
        Set<Long> centros = Set.of(10L);
        AccessContextDTO contexto = new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
        FolhaLinhaSnapshot linha = linha(100L, 10L, "Centro A", 1L, "LN", 200L, "Dev",
            1L, "001", "Salário", "PROVENTO", new BigDecimal("5000.00"));

        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, centros))
            .thenReturn(List.of(linha));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(any(), any(), any(), eq(contexto)))
            .thenReturn(new BigDecimal("5500.00"));
        when(beneficioConsultaPort.contarLancamentosAtivosNaCompetenciaPorCentros(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
            .thenReturn(2L);
        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of());

        DashboardStatsDTO stats = aggregator.aggregateForCompetencia(
            contexto, centros, COMPETENCIA_INICIO, COMPETENCIA_FIM, false);

        assertEquals(1L, stats.totalFuncionarios());
        assertEquals(new BigDecimal("5500.00"), stats.custoMensalFolha());
        assertEquals(2L, stats.totalBeneficiosAtivos());
    }

    @Test
    void evolucaoMeses_retornaSeisMesesTerminandoNaCompetencia() {
        AccessContextDTO contexto = new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
        LocalDate fim = LocalDate.of(2024, 6, 30);
        LocalDate mes1 = LocalDate.of(2024, 1, 1);
        LocalDate mes6 = LocalDate.of(2024, 6, 1);

        FolhaEvolucaoSnapshot jan = evolucao(mes1, LocalDate.of(2024, 1, 31), new BigDecimal("1000"), 10);
        FolhaEvolucaoSnapshot jun = evolucao(mes6, fim, new BigDecimal("6000"), 60);
        FolhaEvolucaoSnapshot dez = evolucao(LocalDate.of(2024, 12, 1), LocalDate.of(2024, 12, 31),
            new BigDecimal("12000"), 120);

        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(jan, jun, dez));

        List<EvolucaoMensalDTO> evolucao = aggregator.evolucaoMeses(contexto, null, fim, 6, false);

        assertFalse(evolucao.isEmpty());
        assertEquals(2, evolucao.size());
        assertEquals(new BigDecimal("1000"), evolucao.get(0).valorTotal());
        assertEquals(new BigDecimal("6000"), evolucao.get(1).valorTotal());
    }

    @Test
    void evolucaoMeses_scoped_recalculaPorCentros() {
        Set<Long> centros = Set.of(10L);
        AccessContextDTO contexto = new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
        LocalDate fim = LocalDate.of(2024, 6, 30);
        FolhaEvolucaoSnapshot jun = evolucao(COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("99999"), 100);
        FolhaLinhaSnapshot linha = linha(100L, 10L, "CC", 1L, "LN", 200L, "Dev",
            1L, "001", "Sal", "PROVENTO", new BigDecimal("3000"));

        when(folhaConsultaPort.findEvolucaoUltimos12Meses(any())).thenReturn(List.of(jun));
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, centros))
            .thenReturn(List.of(linha));
        when(folhaTotalizacaoPort.calcularTotalCustoEmpresa(any(), any(), any(), isNull()))
            .thenReturn(new BigDecimal("3500.00"));

        List<EvolucaoMensalDTO> evolucao = aggregator.evolucaoMeses(contexto, centros, fim, 6, false);

        assertEquals(1, evolucao.size());
        assertEquals(new BigDecimal("3500.00"), evolucao.get(0).valorTotal());
        assertEquals(1, evolucao.get(0).quantidadeFuncionarios());
    }

    private FolhaEvolucaoSnapshot evolucao(
            LocalDate inicio, LocalDate fim, BigDecimal total, int empregados) {
        return new FolhaEvolucaoSnapshot(inicio, fim, total, empregados, false);
    }

    private FolhaLinhaSnapshot linha(
            Long funcionarioId, Long centroId, String centroDesc,
            Long linhaId, String linhaDesc, Long cargoId, String cargoDesc,
            Long rubricaId, String rubricaCodigo, String rubricaDesc,
            String tipo, BigDecimal valor) {
        short ob = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        short ol = "DESCONTO".equals(tipo) ? (short) -1 : (short) 1;
        short oc = (short) 1;
        return new FolhaLinhaSnapshot(
            funcionarioId, "Func", centroId, centroDesc, linhaId, linhaDesc,
            cargoId, cargoDesc, rubricaId, rubricaCodigo, rubricaDesc, tipo, valor,
            ob, ol, oc, OrigemLinha.FOLHA_ADP, null);
    }
}
