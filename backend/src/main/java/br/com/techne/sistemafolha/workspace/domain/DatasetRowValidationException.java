package br.com.techne.sistemafolha.workspace.domain;

import java.util.List;

public class DatasetRowValidationException extends RuntimeException {

    private final List<FieldValidationError> errors;

    public DatasetRowValidationException(List<FieldValidationError> errors) {
        super("Valores da linha inválidos");
        this.errors = List.copyOf(errors);
    }

    public List<FieldValidationError> getErrors() {
        return errors;
    }
}
