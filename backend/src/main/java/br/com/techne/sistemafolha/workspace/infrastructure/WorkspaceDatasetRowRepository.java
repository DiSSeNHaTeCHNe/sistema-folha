package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceDatasetRowRepository extends JpaRepository<WorkspaceDatasetRow, Long> {

    List<WorkspaceDatasetRow> findByDatasetIdOrderByOrdemAscIdAsc(Long datasetId);

    long countByDatasetId(Long datasetId);
}
