package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceTemplateInstallationNotFoundException extends RuntimeException {

    public WorkspaceTemplateInstallationNotFoundException(Long id) {
        super("Instalação de template não encontrada: " + id);
    }
}
