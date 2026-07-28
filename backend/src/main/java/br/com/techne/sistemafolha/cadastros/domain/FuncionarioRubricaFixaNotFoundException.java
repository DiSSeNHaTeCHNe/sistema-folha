package br.com.techne.sistemafolha.cadastros.domain;

public class FuncionarioRubricaFixaNotFoundException extends RuntimeException {

    public FuncionarioRubricaFixaNotFoundException(Long id) {
        super("Rubrica fixa não encontrada: " + id);
    }
}
