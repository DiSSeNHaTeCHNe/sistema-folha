package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaConsultaAdapterTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @InjectMocks
    private FolhaConsultaAdapter adapter;

    @Test
    void findResumoMaisRecente_comResumo_retornaSnapshot() {
        ResumoFolhaPagamento resumo = resumo(COMPETENCIA_INICIO, COMPETENCIA_FIM, new BigDecimal("50000.00"), 10, false);
        when(resumoFolhaPagamentoRepository.findByCompetenciaMaisRecente()).thenReturn(List.of(resumo));

        Optional<FolhaResumoSnapshot> result = adapter.findResumoMaisRecente();

        assertTrue(result.isPresent());
        assertEquals(COMPETENCIA_INICIO, result.get().competenciaInicio());
        assertEquals(new BigDecimal("50000.00"), result.get().totalLiquido());
        assertEquals(10, result.get().totalEmpregados());
    }

    @Test
    void findResumoMaisRecente_semResumo_retornaEmpty() {
        when(resumoFolhaPagamentoRepository.findByCompetenciaMaisRecente()).thenReturn(List.of());

        Optional<FolhaResumoSnapshot> result = adapter.findResumoMaisRecente();

        assertTrue(result.isEmpty());
    }

    @Test
    void findLinhasAtivasPorCompetencia_filtraPorCentroCusto() {
        FolhaPagamento linhaCc1 = folhaPagamento(1L, 100L, "CC Alpha", new BigDecimal("1000.00"));
        FolhaPagamento linhaCc2 = folhaPagamento(2L, 200L, "CC Beta", new BigDecimal("2000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndAtivoTrue(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(linhaCc1, linhaCc2));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, Set.of(100L));

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).centroCustoId());
        assertEquals(new BigDecimal("1000.00"), result.get(0).valor());
    }

    @Test
    void findLinhasAtivasPorCompetencia_centrosNull_retornaTodas() {
        FolhaPagamento linhaCc1 = folhaPagamento(1L, 100L, "CC Alpha", new BigDecimal("1000.00"));
        FolhaPagamento linhaCc2 = folhaPagamento(2L, 200L, "CC Beta", new BigDecimal("2000.00"));

        when(folhaPagamentoRepository.findByCompetenciaAndAtivoTrue(COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(List.of(linhaCc1, linhaCc2));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, null);

        assertEquals(2, result.size());
    }

    private ResumoFolhaPagamento resumo(
            LocalDate inicio, LocalDate fim, BigDecimal totalLiquido, int empregados, boolean decimoTerceiro) {
        ResumoFolhaPagamento resumo = new ResumoFolhaPagamento();
        resumo.setCompetenciaInicio(inicio);
        resumo.setCompetenciaFim(fim);
        resumo.setTotalLiquido(totalLiquido);
        resumo.setTotalEmpregados(empregados);
        resumo.setDecimoTerceiro(decimoTerceiro);
        resumo.setAtivo(true);
        return resumo;
    }

    private FolhaPagamento folhaPagamento(Long funcionarioId, Long centroCustoId, String centroDescricao, BigDecimal valor) {
        LinhaNegocio linhaNegocio = new LinhaNegocio();
        linhaNegocio.setId(10L);
        linhaNegocio.setDescricao("LN Teste");

        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(centroCustoId);
        centroCusto.setDescricao(centroDescricao);
        centroCusto.setLinhaNegocio(linhaNegocio);

        Cargo cargo = new Cargo();
        cargo.setId(5L);
        cargo.setDescricao("Analista");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(funcionarioId);
        funcionario.setNome("Func " + funcionarioId);
        funcionario.setCentroCusto(centroCusto);
        funcionario.setCargo(cargo);

        TipoRubrica tipoRubrica = new TipoRubrica();
        tipoRubrica.setDescricao("PROVENTO");

        Rubrica rubrica = new Rubrica();
        rubrica.setId(1L);
        rubrica.setCodigo("0010");
        rubrica.setDescricao("Salário Base");
        rubrica.setTipoRubrica(tipoRubrica);

        FolhaPagamento folha = new FolhaPagamento();
        folha.setFuncionario(funcionario);
        folha.setRubrica(rubrica);
        folha.setCargo(cargo);
        folha.setCentroCusto(centroCusto);
        folha.setLinhaNegocio(linhaNegocio);
        folha.setValor(valor);
        folha.setDataInicio(COMPETENCIA_INICIO);
        folha.setDataFim(COMPETENCIA_FIM);
        folha.setAtivo(true);
        return folha;
    }
}
