package br.com.techne.sistemafolha.workspace.domain.formula;

public class FormulaParseException extends RuntimeException {

    public FormulaParseException(String message) {
        super(message);
    }

    public FormulaParseException(String message, int position) {
        super(message + " (posição " + position + ")");
    }
}
