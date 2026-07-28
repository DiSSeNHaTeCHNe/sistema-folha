package br.com.techne.sistemafolha.cadastros.domain;

public class FuncionarioRubricaFixaVigenciaConflictException extends RuntimeException {

    public FuncionarioRubricaFixaVigenciaConflictException() {
        super("Já existe rubrica fixa ativa com vigência sobreposta para este funcionário e rubrica");
    }
}
