package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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

    private static FolhaLinhaSnapshot linha(Long funcionarioId, String tipo, String valor) {
        short ob = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        short ol = "DESCONTO".equals(tipo) ? (short) -1 : ("PROVENTO".equals(tipo) ? (short) 1 : (short) 0);
        short oc = "PROVENTO".equals(tipo) ? (short) 1 : (short) 0;
        return new FolhaLinhaSnapshot(
            funcionarioId, 1L, "CC", 1L, "LN", 1L, "Cargo",
            1L, "001", "Rubrica", tipo, new BigDecimal(valor), ob, ol, oc, OrigemLinha.FOLHA_ADP);
    }
}
