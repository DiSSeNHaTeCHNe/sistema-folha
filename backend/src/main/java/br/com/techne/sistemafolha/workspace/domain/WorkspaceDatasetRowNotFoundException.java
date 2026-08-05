package br.com.techne.sistemafolha.workspace.domain;

public class WorkspaceDatasetRowNotFoundException extends RuntimeException {

    public WorkspaceDatasetRowNotFoundException(Long datasetId, Long rowId) {
        super("Linha não encontrada: dataset=" + datasetId + ", linha=" + rowId);
    }
}
