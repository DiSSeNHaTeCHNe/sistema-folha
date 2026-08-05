package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceNameConflictException extends RuntimeException {

    public WorkspaceNameConflictException(String nome) {
        super("Já existe workspace com o nome: " + nome);
    }
}
