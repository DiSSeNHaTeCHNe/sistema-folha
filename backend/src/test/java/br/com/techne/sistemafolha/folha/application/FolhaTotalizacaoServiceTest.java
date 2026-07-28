package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaTotalizacaoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private BeneficioConsultaPort beneficioConsultaPort;

    @InjectMocks
    private FolhaTotalizacaoService folhaTotalizacaoService;

    @Test
    void calcularTotaisPorFuncionario_quandoPortRetornaLancamentos_somaBeneficiosMensais() {
        Funcionario funcionario = funcionario(1L, "Ana Silva");
        FolhaPagamento linha = linhaFolha(funcionario, new BigDecimal("8000.00"));

        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(2);
        when(beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(new BigDecimal("700.00"));

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
            .calcularTotaisPorFuncionario(List.of(linha));

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("8000.00"), total.salCustoFolha());
        assertEquals(new BigDecimal("700.00"), total.salCustoBeneficios());
        assertEquals(new BigDecimal("8700.00"), total.salCustoTechne());
        assertEquals(2, total.totalBeneficios());

        verify(beneficioConsultaPort).somarValorPorFuncionarioECompetencia(
            eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM));
    }

    @Test
    void calcularTotaisPorFuncionario_quandoPortRetornaZero_custoBeneficiosZero() {
        Funcionario funcionario = funcionario(2L, "Bruno Costa");
        FolhaPagamento linha = linhaFolha(funcionario, new BigDecimal("6000.00"));

        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                2L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);
        when(beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                2L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(BigDecimal.ZERO);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
            .calcularTotaisPorFuncionario(List.of(linha));

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("6000.00"), total.salCustoFolha());
        assertEquals(BigDecimal.ZERO.setScale(2), total.salCustoBeneficios());
        assertEquals(new BigDecimal("6000.00"), total.salCustoTechne());
        assertEquals(0, total.totalBeneficios());
    }

    @Test
    void calcularTotaisPorFuncionario_listaVazia_retornaVazio() {
        assertEquals(List.of(), folhaTotalizacaoService.calcularTotaisPorFuncionario(List.of()));
    }

    @Test
    void calcularTotaisPorFuncionario_funcionarioSemLancamentoNaCompetencia_custoBeneficiosZero() {
        Funcionario funcionario = funcionario(3L, "Carla Dias");
        FolhaPagamento linha = linhaFolha(funcionario, new BigDecimal("5000.00"));

        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                3L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);
        when(beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                3L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(BigDecimal.ZERO);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
            .calcularTotaisPorFuncionario(List.of(linha));

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(BigDecimal.ZERO.setScale(2), total.salCustoBeneficios());
        assertEquals(new BigDecimal("5000.00"), total.salCustoTechne());
        assertEquals(0, total.totalBeneficios());
    }

    @Test
    void calcularTotaisPorFuncionario_proventoEDesconto_descontoImpactaSomenteLiquido() {
        Funcionario funcionario = funcionario(4L, "Diana Provento");
        FolhaPagamento provento = linhaFolha(funcionario, rubricaComOperadores((short) 1, (short) 1, (short) 1),
            new BigDecimal("10000.00"));
        FolhaPagamento desconto = linhaFolha(funcionario, rubricaComOperadores((short) 0, (short) -1, (short) 0),
            new BigDecimal("1500.00"));

        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                4L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);
        when(beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                4L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(BigDecimal.ZERO);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
            .calcularTotaisPorFuncionario(List.of(provento, desconto));

        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("10000.00"), total.salBruto());
        assertEquals(new BigDecimal("8500.00"), total.salLiquido());
        assertEquals(new BigDecimal("10000.00"), total.salCustoFolha());
    }

    @Test
    void calcularTotaisPorFuncionario_operadorCustom_descontoSomenteLiquido() {
        Funcionario funcionario = funcionario(5L, "Eduardo Custom");
        FolhaPagamento linha = linhaFolha(funcionario, rubricaComOperadores((short) 0, (short) -1, (short) 0),
            new BigDecimal("500.00"));

        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                5L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);
        when(beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                5L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(BigDecimal.ZERO);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
            .calcularTotaisPorFuncionario(List.of(linha));

        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("0.00"), total.salBruto());
        assertEquals(new BigDecimal("-500.00"), total.salLiquido());
        assertEquals(new BigDecimal("0.00"), total.salCustoFolha());
    }

    @Test
    void calcularTotaisPorFuncionario_arredondaHalfUpDuasCasas() {
        Funcionario funcionario = funcionario(6L, "Fabio Arredonda");
        FolhaPagamento linha = linhaFolha(funcionario, rubricaComOperadores((short) 1, (short) 1, (short) 1),
            new BigDecimal("10.005"));

        when(beneficioConsultaPort.contarLancamentosPorFuncionarioECompetencia(
                6L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(0);
        when(beneficioConsultaPort.somarValorPorFuncionarioECompetencia(
                6L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(BigDecimal.ZERO);

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
            .calcularTotaisPorFuncionario(List.of(linha));

        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("10.01"), total.salBruto());
        assertEquals(new BigDecimal("10.01"), total.salLiquido());
        assertEquals(new BigDecimal("10.01"), total.salCustoFolha());
    }

    private Funcionario funcionario(Long id, String nome) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome(nome);
        return funcionario;
    }

    private FolhaPagamento linhaFolha(Funcionario funcionario, Rubrica rubrica, BigDecimal valor) {
        FolhaPagamento linha = new FolhaPagamento();
        linha.setFuncionario(funcionario);
        linha.setRubrica(rubrica);
        linha.setValor(valor);
        linha.setDataInicio(COMPETENCIA_INICIO);
        linha.setDataFim(COMPETENCIA_FIM);
        return linha;
    }

    private FolhaPagamento linhaFolha(Funcionario funcionario, BigDecimal valor) {
        return linhaFolha(funcionario, rubricaProvento(), valor);
    }

    private Rubrica rubricaProvento() {
        return rubricaComOperadores((short) 1, (short) 1, (short) 1);
    }

    private Rubrica rubricaComOperadores(short bruto, short liquido, short custo) {
        TipoRubrica tipoProvento = new TipoRubrica();
        tipoProvento.setDescricao("PROVENTO");

        Rubrica rubrica = new Rubrica();
        rubrica.setTipoRubrica(tipoProvento);
        rubrica.setOperadorBruto(bruto);
        rubrica.setOperadorLiquido(liquido);
        rubrica.setOperadorCusto(custo);
        return rubrica;
    }
}
