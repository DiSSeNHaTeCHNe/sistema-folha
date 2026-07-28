package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FolhaLinhaAgregacaoTest {

    private final FolhaLinhaAgregacao agregacao = new FolhaLinhaAgregacao();

    @Test
    void agregar_listaVazia_retornaTodosZeros() {
        FolhaLinhaAgregacao.Totais totais = agregacao.agregar(List.of());

        assertEquals(0L, totais.empregados());
        assertEquals(0, BigDecimal.ZERO.compareTo(totais.pagamentos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(totais.descontos()));
        assertEquals(0, BigDecimal.ZERO.compareTo(totais.liquido()));
    }

    @Test
    void agregar_happyPath_distinctEmpregadosESomasProventoDescontoLiquido() {
        List<FolhaLinhaSnapshot> linhas = List.of(
            linha(1L, "PROVENTO", "5000.00"),
            linha(1L, "DESCONTO", "500.00"),
            linha(2L, "PROVENTO", "3000.00"),
            linha(2L, "DESCONTO", "200.00")
        );

        FolhaLinhaAgregacao.Totais totais = agregacao.agregar(linhas);

        assertEquals(2L, totais.empregados());
        assertEquals(0, new BigDecimal("8000.00").compareTo(totais.pagamentos()));
        assertEquals(0, new BigDecimal("700.00").compareTo(totais.descontos()));
        assertEquals(0, new BigDecimal("7300.00").compareTo(totais.liquido()));
    }

    @Test
    void agregar_mixDeTipos_ignoraTiposQueNaoSaoProventoNemDesconto() {
        List<FolhaLinhaSnapshot> linhas = List.of(
            linha(10L, "PROVENTO", "1000.00"),
            linha(10L, "DESCONTO", "100.00"),
            linha(10L, "ENCARGO", "50.00"),
            linha(20L, "OUTRO", "999.00"),
            linha(20L, "PROVENTO", "200.00")
        );

        FolhaLinhaAgregacao.Totais totais = agregacao.agregar(linhas);

        assertEquals(2L, totais.empregados());
        assertEquals(0, new BigDecimal("1200.00").compareTo(totais.pagamentos()));
        assertEquals(0, new BigDecimal("100.00").compareTo(totais.descontos()));
        assertEquals(0, new BigDecimal("1100.00").compareTo(totais.liquido()));
    }

    @Test
    void agregarComBeneficiosEEncargos_listaVazia_retornaZeros() {
        FolhaLinhaAgregacao.TotaisResumo totais = agregacao.agregar(List.of(), Map.of(), Map.of());

        assertEquals(0L, totais.empregados());
        assertEquals(0, BigDecimal.ZERO.compareTo(totais.totalBruto()));
        assertEquals(0, BigDecimal.ZERO.compareTo(totais.totalCustoEmpresa()));
    }

    @Test
    void agregarComBeneficiosEEncargos_usaOperadoresEMotor() {
        List<FolhaLinhaSnapshot> linhas = List.of(
            linhaOperadores(1L, "5000.00", (short) 1, (short) 1, (short) 1),
            linhaOperadores(1L, "500.00", (short) 0, (short) -1, (short) 0),
            linhaOperadores(2L, "3000.00", (short) 1, (short) 1, (short) 1)
        );
        Map<Long, BigDecimal> beneficios = Map.of(1L, new BigDecimal("200.00"), 2L, new BigDecimal("100.00"));
        Map<Long, BigDecimal> encargos = Map.of(1L, new BigDecimal("800.00"), 2L, new BigDecimal("200.00"));

        FolhaLinhaAgregacao.TotaisResumo totais = agregacao.agregar(linhas, beneficios, encargos);

        assertEquals(2L, totais.empregados());
        assertEquals(0, new BigDecimal("8000.00").compareTo(totais.totalBruto()));
        assertEquals(0, new BigDecimal("7500.00").compareTo(totais.totalLiquido()));
        assertEquals(0, new BigDecimal("8000.00").compareTo(totais.totalCustoFolha()));
        assertEquals(0, new BigDecimal("300.00").compareTo(totais.totalCustoBeneficios()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(totais.totalEncargos()));
        assertEquals(0, new BigDecimal("9300.00").compareTo(totais.totalCustoEmpresa()));
    }

    @Test
    void agregarComBeneficiosEEncargos_scoped_encargosZero_custoEmpresaIncluiBeneficios() {
        List<FolhaLinhaSnapshot> linhas = List.of(
            linhaOperadores(5L, "4000.00", (short) 1, (short) 1, (short) 1)
        );
        Map<Long, BigDecimal> beneficios = Map.of(5L, new BigDecimal("150.00"));

        FolhaLinhaAgregacao.TotaisResumo totais = agregacao.agregar(linhas, beneficios, Map.of());

        assertEquals(0, BigDecimal.ZERO.compareTo(totais.totalEncargos()));
        assertEquals(0, new BigDecimal("4150.00").compareTo(totais.totalCustoEmpresa()));
    }

    private static FolhaLinhaSnapshot linha(Long funcionarioId, String tipo, String valor) {
        short ob = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        short ol = "DESCONTO".equals(tipo) ? (short) -1 : ("PROVENTO".equals(tipo) ? (short) 1 : (short) 0);
        short oc = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        return new FolhaLinhaSnapshot(
            funcionarioId, "Func " + funcionarioId, 1L, "CC", 1L, "LN", 1L, "Cargo",
            1L, "001", "Rubrica", tipo, new BigDecimal(valor), ob, ol, oc, OrigemLinha.FOLHA_ADP);
    }

    private static FolhaLinhaSnapshot linhaOperadores(
            Long funcionarioId, String valor, short ob, short ol, short oc) {
        return new FolhaLinhaSnapshot(
            funcionarioId, "Func " + funcionarioId, 1L, "CC", 1L, "LN", 1L, "Cargo",
            1L, "001", "Rubrica", "PROVENTO", new BigDecimal(valor), ob, ol, oc, OrigemLinha.FOLHA_ADP);
    }
}
