package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceProposalAlreadyAppliedException extends RuntimeException {

    public WorkspaceProposalAlreadyAppliedException() {
        super("Proposta já foi aplicada ou descartada");
    }
}
