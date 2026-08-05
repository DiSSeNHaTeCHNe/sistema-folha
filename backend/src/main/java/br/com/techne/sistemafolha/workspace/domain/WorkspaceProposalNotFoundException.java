package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceProposalNotFoundException extends RuntimeException {

    public WorkspaceProposalNotFoundException(Long id) {
        super("Proposta não encontrada: " + id);
    }
}
