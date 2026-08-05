package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.formula.EvaluationContext;
import br.com.techne.sistemafolha.workspace.domain.formula.TypedValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaEngineEvaluateTest {

    private FormulaEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FormulaEngine();
    }

    @Test
    void evaluate_somaVezesMedia_parityManual() {
        EvaluationContext ctx = EvaluationContext.builder()
            .putSeries("a", List.of(new BigDecimal("10"), new BigDecimal("20")))
            .putSeries("b", List.of(new BigDecimal("2"), new BigDecimal("4")))
            .build();

        TypedValue result = engine.evaluate("SOMA(a) * MÉDIA(b)", ctx);

        assertEquals(0, new BigDecimal("90").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_soma_agregaValores() {
        EvaluationContext ctx = context("valor", List.of("5", "15"));

        TypedValue result = engine.evaluate("SOMA(valor)", ctx);

        assertEquals(0, new BigDecimal("20").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_media_calculaMedia() {
        EvaluationContext ctx = context("valor", List.of("10", "20", "30"));

        TypedValue result = engine.evaluate("MÉDIA(valor)", ctx);

        assertEquals(0, new BigDecimal("20").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_minMax_retornaExtremos() {
        EvaluationContext ctx = context("valor", List.of("3", "9", "1"));

        assertEquals(0, new BigDecimal("1").compareTo(engine.evaluate("MÍN(valor)", ctx).asNumber()));
        assertEquals(0, new BigDecimal("9").compareTo(engine.evaluate("MÁX(valor)", ctx).asNumber()));
    }

    @Test
    void evaluate_contagem_retornaQuantidade() {
        EvaluationContext ctx = context("valor", List.of("1", "2", "3", "4"));

        TypedValue result = engine.evaluate("CONTAGEM(valor)", ctx);

        assertEquals(0, new BigDecimal("4").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_seVerdadeiro_retornaPrimeiroBranch() {
        EvaluationContext ctx = context("a", List.of("10"));

        TypedValue result = engine.evaluate("SE(SOMA(a) > 5, 100, 0)", ctx);

        assertEquals(0, new BigDecimal("100").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_seFalso_retornaSegundoBranch() {
        EvaluationContext ctx = context("a", List.of("2"));

        TypedValue result = engine.evaluate("SE(SOMA(a) > 5, 100, 50)", ctx);

        assertEquals(0, new BigDecimal("50").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_seAninhado_avaliaCorretamente() {
        EvaluationContext ctx = EvaluationContext.builder()
            .putSeries("a", List.of(new BigDecimal("1")))
            .putSeries("b", List.of(new BigDecimal("10")))
            .build();

        TypedValue result = engine.evaluate("SE(SOMA(a) > 0, SE(MÉDIA(b) >= 10, 1, 2), 3)", ctx);

        assertEquals(0, new BigDecimal("1").compareTo(result.asNumber()));
    }

    @Test
    void evaluate_divisaoPorZero_retornaZero() {
        TypedValue result = engine.evaluate("10 / 0", EvaluationContext.builder().build());

        assertEquals(0, BigDecimal.ZERO.compareTo(result.asNumber()));
    }

    @Test
    void evaluate_aritmeticaBasica() {
        EvaluationContext ctx = EvaluationContext.builder().build();

        assertEquals(0, new BigDecimal("7").compareTo(engine.evaluate("3 + 4", ctx).asNumber()));
        assertEquals(0, new BigDecimal("2").compareTo(engine.evaluate("5 - 3", ctx).asNumber()));
        assertEquals(0, new BigDecimal("12").compareTo(engine.evaluate("3 * 4", ctx).asNumber()));
        assertEquals(0, new BigDecimal("2.5").compareTo(engine.evaluate("5 / 2", ctx).asNumber()));
    }

    @Test
    void evaluate_comparadores_retornaBooleanoNoSe() {
        EvaluationContext ctx = context("x", List.of("5"));

        assertTrue(engine.evaluate("SE(SOMA(x) = 5, 1, 0)", ctx).asNumber().intValue() == 1);
        assertTrue(engine.evaluate("SE(SOMA(x) <> 5, 1, 0)", ctx).asNumber().intValue() == 0);
        assertTrue(engine.evaluate("SE(SOMA(x) < 10, 1, 0)", ctx).asNumber().intValue() == 1);
        assertTrue(engine.evaluate("SE(SOMA(x) >= 5, 1, 0)", ctx).asNumber().intValue() == 1);
    }

    @Test
    void evaluate_serieVazia_retornaZero() {
        EvaluationContext ctx = EvaluationContext.builder().build();

        TypedValue result = engine.evaluate("SOMA(valor)", ctx);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.asNumber()));
    }

    @Test
    void evaluate_comparadorRetornaBooleano() {
        EvaluationContext ctx = EvaluationContext.builder().build();

        assertTrue(engine.evaluate("1 > 0", ctx).asBoolean());
        assertFalse(engine.evaluate("1 < 0", ctx).asBoolean());
    }

    private EvaluationContext context(String field, List<String> values) {
        EvaluationContext.Builder builder = EvaluationContext.builder();
        builder.putSeries(field, values.stream().map(BigDecimal::new).toList());
        return builder.build();
    }
}
