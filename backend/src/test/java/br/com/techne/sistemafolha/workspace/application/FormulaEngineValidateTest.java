package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaEngineValidateTest {

    private FormulaEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FormulaEngine();
    }

    @Test
    void validate_aritmeticaSimples_valida() {
        FormulaValidationResult result = engine.validate("1 + 2 * 3", fields("a"));

        assertTrue(result.valid());
    }

    @Test
    void validate_somaCampo_valida() {
        FormulaValidationResult result = engine.validate("SOMA(valor)", fields("valor"));

        assertTrue(result.valid());
    }

    @Test
    void validate_mediaCampo_valida() {
        FormulaValidationResult result = engine.validate("MÉDIA(valor)", fields("valor"));

        assertTrue(result.valid());
    }

    @Test
    void validate_minMaxContagem_valida() {
        assertTrue(engine.validate("MÍN(valor)", fields("valor")).valid());
        assertTrue(engine.validate("MÁX(valor)", fields("valor")).valid());
        assertTrue(engine.validate("CONTAGEM(valor)", fields("valor")).valid());
    }

    @Test
    void validate_expressaoCompostaSomaMedia_valida() {
        FormulaValidationResult result = engine.validate("SOMA(a) * MÉDIA(b)", fields("a", "b"));

        assertTrue(result.valid());
    }

    @Test
    void validate_seAninhado_valida() {
        FormulaValidationResult result = engine.validate(
            "SE(SOMA(a) > MÉDIA(b), SOMA(c), 0)", fields("a", "b", "c"));

        assertTrue(result.valid());
    }

    @Test
    void validate_campoSistema_valida() {
        FormulaValidationResult result = engine.validate(
            "SOMA(sistema.folha.headcount)", fields("sistema.folha.headcount"));

        assertTrue(result.valid());
    }

    @Test
    void validate_funcaoDesconhecida_rejeita() {
        FormulaValidationResult result = engine.validate("SUBTOTAL(valor)", fields("valor"));

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("SUBTOTAL")));
    }

    @Test
    void validate_runtime_rejeita() {
        FormulaValidationResult result = engine.validate("Runtime.getRuntime()", fields("valor"));

        assertFalse(result.valid());
    }

    @Test
    void validate_eval_rejeita() {
        FormulaValidationResult result = engine.validate("eval('1')", fields("valor"));

        assertFalse(result.valid());
    }

    @Test
    void validate_campoInexistente_rejeita() {
        FormulaValidationResult result = engine.validate("SOMA(inexistente)", fields("valor"));

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("inexistente")));
    }

    @Test
    void validate_expressaoVazia_rejeita() {
        FormulaValidationResult result = engine.validate("   ", fields("valor"));

        assertFalse(result.valid());
    }

    @Test
    void validate_seArgumentosInvalidos_rejeita() {
        FormulaValidationResult result = engine.validate("SE(SOMA(a) > 0, 1)", fields("a"));

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(e -> e.contains("3 argumentos")));
    }

    @Test
    void validate_agregacaoSemCampo_rejeita() {
        FormulaValidationResult result = engine.validate("SOMA(1 + 2)", fields("valor"));

        assertFalse(result.valid());
    }

    @Test
    void validate_sintaxeInvalida_rejeita() {
        FormulaValidationResult result = engine.validate("SOMA(valor", fields("valor"));

        assertFalse(result.valid());
    }

    @Test
    void validate_comparadoresEmSe_valida() {
        FormulaValidationResult result = engine.validate(
            "SE(MÉDIA(a) >= 10, 1, 0)", fields("a"));

        assertTrue(result.valid());
    }

    private List<AvailableField> fields(String... names) {
        return java.util.Arrays.stream(names)
            .map(AvailableField::new)
            .toList();
    }
}
