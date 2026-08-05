package br.com.techne.sistemafolha.workspace.domain.formula;

import java.util.List;

public class FormulaValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private FormulaValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
    }

    public static FormulaValidationResult ok() {
        return new FormulaValidationResult(true, List.of());
    }

    public static FormulaValidationResult invalid(String error) {
        return new FormulaValidationResult(false, List.of(error));
    }

    public static FormulaValidationResult invalid(List<String> errors) {
        return new FormulaValidationResult(false, List.copyOf(errors));
    }

    public boolean valid() {
        return valid;
    }

    public List<String> errors() {
        return errors;
    }
}
