package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(Long id) {
        super("Workspace não encontrado: " + id);
    }
}
