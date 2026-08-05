package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceDatasetRowRepository extends JpaRepository<WorkspaceDatasetRow, Long> {

    List<WorkspaceDatasetRow> findByDatasetIdOrderByOrdemAscIdAsc(Long datasetId);

    Optional<WorkspaceDatasetRow> findByDatasetIdAndId(Long datasetId, Long id);

    long countByDatasetId(Long datasetId);

    @Query("""
        SELECT r.datasetId AS datasetId, MAX(r.dataAtualizacao) AS maxDataAtualizacao
        FROM WorkspaceDatasetRow r
        WHERE r.datasetId IN :datasetIds
        GROUP BY r.datasetId
        """)
    List<DatasetRowMaxUpdateProjection> findMaxDataAtualizacaoByDatasetIdIn(
        @Param("datasetIds") Collection<Long> datasetIds);
}
