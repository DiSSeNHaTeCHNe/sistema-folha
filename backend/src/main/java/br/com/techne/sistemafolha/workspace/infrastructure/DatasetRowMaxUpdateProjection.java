package br.com.techne.sistemafolha.workspace.infrastructure;

import java.time.LocalDateTime;

public interface DatasetRowMaxUpdateProjection {

    Long getDatasetId();

    LocalDateTime getMaxDataAtualizacao();
}
