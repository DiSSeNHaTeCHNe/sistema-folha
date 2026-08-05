package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceTemplateNotFoundException extends RuntimeException {

    public WorkspaceTemplateNotFoundException(Long id) {
        super("Template não encontrado: " + id);
    }
}
