package br.com.techne.sistemafolha.workspace.domain;

import java.util.List;

public class InvalidFormulaException extends RuntimeException {

    private final List<String> errors;

    public InvalidFormulaException(List<String> errors) {
        super("Fórmula inválida");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
