package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaConsultaAdapterTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaPagamentoRepository folhaPagamentoRepository;

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @Mock
    private FichaMensalRepository fichaMensalRepository;

    @Mock
    private FichaLinhaRepository fichaLinhaRepository;

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
    void findLinhasAtivasPorCompetencia_linhaCcDiferenteDoFuncionarioAtual_filtraPorCcDaLinha_fcc06() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);

        CentroCusto ccLinha = new CentroCusto();
        ccLinha.setId(100L);
        ccLinha.setDescricao("CC Alpha");
        ccLinha.setLinhaNegocio(linhaNegocio(10L));

        CentroCusto ccFuncionarioAtual = new CentroCusto();
        ccFuncionarioAtual.setId(200L);
        ccFuncionarioAtual.setDescricao("CC Beta");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(50L);
        funcionario.setNome("Transferido");
        funcionario.setCentroCusto(ccFuncionarioAtual);
        funcionario.setCargo(cargo(5L));

        FolhaPagamento folha = folhaPagamento(50L, 100L, "CC Alpha", new BigDecimal("1000.00"), false);
        folha.setFuncionario(funcionario);
        folha.setCentroCusto(ccLinha);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroWithFetch(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(folha));

        List<FolhaLinhaSnapshot> gestorA = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L));
        assertEquals(1, gestorA.size());
        assertEquals(100L, gestorA.get(0).centroCustoId());

        List<FolhaLinhaSnapshot> gestorB = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(200L));
        assertTrue(gestorB.isEmpty());
    }

    @Test
    void findLinhasAtivasPorCompetencia_semFicha_fallbackAdpFiltraPorCentroCusto() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        FolhaPagamento linhaCc1 = folhaPagamento(1L, 100L, "CC Alpha", new BigDecimal("1000.00"), false);
        FolhaPagamento linhaCc2 = folhaPagamento(2L, 200L, "CC Beta", new BigDecimal("2000.00"), false);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroWithFetch(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaCc1, linhaCc2));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L));

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).centroCustoId());
        assertEquals(new BigDecimal("1000.00"), result.get(0).valor());
        assertEquals(OrigemLinha.FOLHA_ADP, result.get(0).origemLinha());
        assertEquals((short) 1, result.get(0).operadorBruto());
    }

    @Test
    void findLinhasAtivasPorCompetencia_comFicha_usaFichaLinhaComOperadoresSnapshot() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(true);
        FichaLinha linhaFicha = fichaLinha(1L, 100L, new BigDecimal("1500.00"), (short) 1, (short) 1, (short) 1,
            OrigemLinha.CUSTO_FIXO, null);

        when(fichaLinhaRepository.findByCompetenciaAndCentrosCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L)))
            .thenReturn(List.of(linhaFicha));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L));

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("1500.00"), result.get(0).valor());
        assertEquals(OrigemLinha.CUSTO_FIXO, result.get(0).origemLinha());
        verify(folhaPagamentoRepository, never())
            .findByCompetenciaAndDecimoTerceiroWithFetch(any(), any(), anyBoolean());
    }

    @Test
    void findLinhasAtivasPorCompetencia_filtraPorDecimoTerceiro() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, true))
            .thenReturn(false);
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        FolhaPagamento linhaRegular = folhaPagamento(1L, 100L, "CC Alpha", new BigDecimal("1000.00"), false);
        FolhaPagamento linhaDecimo = folhaPagamento(1L, 100L, "CC Alpha", new BigDecimal("3000.00"), true);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroWithFetch(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, true))
            .thenReturn(List.of(linhaDecimo));
        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroWithFetch(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaRegular));

        List<FolhaLinhaSnapshot> regular = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null);
        List<FolhaLinhaSnapshot> decimo = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, true, null);

        assertEquals(1, regular.size());
        assertEquals(new BigDecimal("1000.00"), regular.get(0).valor());
        assertEquals(1, decimo.size());
        assertEquals(new BigDecimal("3000.00"), decimo.get(0).valor());
    }

    @Test
    void findEvolucaoUltimos12Meses_delegaParaFindUltimos12MesesRegulares() {
        LocalDate dataInicio = LocalDate.of(2024, 1, 1);
        LocalDate dezInicio = LocalDate.of(2024, 12, 1);
        LocalDate dezFim = LocalDate.of(2024, 12, 31);
        ResumoFolhaPagamento regular = resumo(dezInicio, dezFim, new BigDecimal("50000.00"), 10, false);

        when(resumoFolhaPagamentoRepository.findUltimos12MesesRegulares(dataInicio))
            .thenReturn(List.of(regular));

        List<FolhaEvolucaoSnapshot> result = adapter.findEvolucaoUltimos12Meses(dataInicio);

        verify(resumoFolhaPagamentoRepository).findUltimos12MesesRegulares(eq(dataInicio));
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("50000.00"), result.get(0).totalLiquido());
        assertEquals(10, result.get(0).totalEmpregados());
        assertEquals(false, result.get(0).decimoTerceiro());
    }

    @Test
    void findLinhasAtivasPorCompetencia_centrosNull_retornaTodasViaFicha() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(true);
        FichaLinha linha1 = fichaLinha(1L, 100L, new BigDecimal("1000.00"), (short) 1, (short) 1, (short) 1,
            OrigemLinha.FOLHA_ADP, null);
        FichaLinha linha2 = fichaLinha(2L, 200L, new BigDecimal("2000.00"), (short) 1, (short) 1, (short) 1,
            OrigemLinha.FOLHA_ADP, null);

        when(fichaLinhaRepository.findByCompetenciaWithFetch(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linha1, linha2));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null);

        assertEquals(2, result.size());
    }

    @Test
    void findLinhasAtivasPorCompetencia_comFicha_custoFixoFiltraPorCentroCusto() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(true);
        FichaLinha linhaCc1 = fichaLinha(1L, 100L, new BigDecimal("500.00"), (short) 1, (short) 1, (short) 1,
            OrigemLinha.CUSTO_FIXO, null);

        when(fichaLinhaRepository.findByCompetenciaAndCentrosCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L)))
            .thenReturn(List.of(linhaCc1));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L));

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).centroCustoId());
        assertEquals(new BigDecimal("500.00"), result.get(0).valor());
        assertEquals(OrigemLinha.CUSTO_FIXO, result.get(0).origemLinha());
        verify(fichaLinhaRepository).findByCompetenciaAndCentrosCustoIds(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, Set.of(100L));
    }

    @Test
    void findLinhasAtivasPorCompetencia_comFicha_expoePorcentagemSnapshot() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(true);
        FichaLinha linhaFicha = fichaLinha(1L, 100L, new BigDecimal("7258.43"), (short) 1, (short) 1, (short) 1,
            OrigemLinha.FOLHA_ADP, new BigDecimal("138.63"));

        when(fichaLinhaRepository.findByCompetenciaWithFetch(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaFicha));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("138.63"), result.get(0).porcentagem());
    }

    @Test
    void findLinhasAtivasPorCompetencia_fallbackAdp_expoePorcentagemLiveDaRubrica() {
        when(fichaMensalRepository.existsByCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(false);
        FolhaPagamento linhaAdp = folhaPagamento(1L, 100L, "CC Alpha", new BigDecimal("7258.43"), false);
        linhaAdp.getRubrica().setPorcentagem(138.63);

        when(folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroWithFetch(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, false))
            .thenReturn(List.of(linhaAdp));

        List<FolhaLinhaSnapshot> result = adapter.findLinhasAtivasPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, null);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("138.63"), result.get(0).porcentagem());
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

    private LinhaNegocio linhaNegocio(Long id) {
        LinhaNegocio linhaNegocio = new LinhaNegocio();
        linhaNegocio.setId(id);
        linhaNegocio.setDescricao("LN Teste");
        return linhaNegocio;
    }

    private Cargo cargo(Long id) {
        Cargo cargo = new Cargo();
        cargo.setId(id);
        cargo.setDescricao("Analista");
        return cargo;
    }

    private FolhaPagamento folhaPagamento(
            Long funcionarioId, Long centroCustoId, String centroDescricao, BigDecimal valor, boolean decimoTerceiro) {
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
        rubrica.setOperadorBruto((short) 1);
        rubrica.setOperadorLiquido((short) 1);
        rubrica.setOperadorCusto((short) 1);

        FolhaPagamento folha = new FolhaPagamento();
        folha.setFuncionario(funcionario);
        folha.setRubrica(rubrica);
        folha.setCargo(cargo);
        folha.setCentroCusto(centroCusto);
        folha.setLinhaNegocio(linhaNegocio);
        folha.setValor(valor);
        folha.setDataInicio(COMPETENCIA_INICIO);
        folha.setDataFim(COMPETENCIA_FIM);
        folha.setDecimoTerceiro(decimoTerceiro);
        folha.setAtivo(true);
        return folha;
    }

    private FichaLinha fichaLinha(
            Long funcionarioId, Long centroCustoId, BigDecimal valor,
            short operadorBruto, short operadorLiquido, short operadorCusto, OrigemLinha origemLinha,
            BigDecimal porcentagem) {
        LinhaNegocio linhaNegocio = new LinhaNegocio();
        linhaNegocio.setId(10L);
        linhaNegocio.setDescricao("LN Teste");

        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(centroCustoId);
        centroCusto.setDescricao("CC");
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

        FichaMensal fichaMensal = new FichaMensal();
        fichaMensal.setFuncionario(funcionario);
        fichaMensal.setCompetenciaInicio(COMPETENCIA_INICIO);
        fichaMensal.setCompetenciaFim(COMPETENCIA_FIM);
        fichaMensal.setDecimoTerceiro(false);
        fichaMensal.setAtivo(true);

        FichaLinha linha = new FichaLinha();
        linha.setFichaMensal(fichaMensal);
        linha.setRubrica(rubrica);
        linha.setValor(valor);
        linha.setOperadorBruto(operadorBruto);
        linha.setOperadorLiquido(operadorLiquido);
        linha.setOperadorCusto(operadorCusto);
        linha.setOrigemLinha(origemLinha);
        linha.setPorcentagem(porcentagem);
        linha.setAtivo(true);
        return linha;
    }
}
