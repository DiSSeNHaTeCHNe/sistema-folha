package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceQuotaExceededException extends RuntimeException {

    public static final String CODE = "WORKSPACE_QUOTA_EXCEEDED";

    public WorkspaceQuotaExceededException(String message) {
        super(message);
    }
}
