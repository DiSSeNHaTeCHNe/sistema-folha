package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceIaPermissaoNegadaException extends RuntimeException {

    public static final String MESSAGE = "Capacidade não disponível";

    public WorkspaceIaPermissaoNegadaException() {
        super(MESSAGE);
    }
}
