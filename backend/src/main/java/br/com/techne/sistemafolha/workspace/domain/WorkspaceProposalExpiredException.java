package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceProposalExpiredException extends RuntimeException {

    public static final String MESSAGE = "Proposta expirada; solicite novamente";

    public WorkspaceProposalExpiredException() {
        super(MESSAGE);
    }
}
