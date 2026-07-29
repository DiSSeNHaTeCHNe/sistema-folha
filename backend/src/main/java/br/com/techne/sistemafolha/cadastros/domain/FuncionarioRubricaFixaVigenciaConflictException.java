package br.com.techne.sistemafolha.cadastros.domain;

public class FuncionarioRubricaFixaVigenciaConflictException extends RuntimeException {

    private FuncionarioRubricaFixaVigenciaConflictException(String message) {
        super(message);
    }

    public static FuncionarioRubricaFixaVigenciaConflictException forIndividual() {
        return new FuncionarioRubricaFixaVigenciaConflictException(
            "Já existe rubrica fixa ativa com vigência sobreposta para este funcionário e rubrica");
    }

    public static FuncionarioRubricaFixaVigenciaConflictException forGlobal() {
        return new FuncionarioRubricaFixaVigenciaConflictException(
            "Já existe rubrica fixa global ativa com vigência sobreposta para esta rubrica");
    }
}
