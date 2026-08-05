package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceWidgetDefinitionNotFoundException extends RuntimeException {

    public WorkspaceWidgetDefinitionNotFoundException(Long id) {
        super("Definição de widget não encontrada: " + id);
    }
}
