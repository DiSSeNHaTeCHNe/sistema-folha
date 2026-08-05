package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceDatasetNotFoundException extends RuntimeException {

    public WorkspaceDatasetNotFoundException(Long id) {
        super("Dataset não encontrado: " + id);
    }
}
