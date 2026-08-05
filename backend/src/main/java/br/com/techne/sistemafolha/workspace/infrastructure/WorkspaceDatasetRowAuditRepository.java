package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceDatasetRowAuditRepository extends JpaRepository<WorkspaceDatasetRowAudit, Long> {

    List<WorkspaceDatasetRowAudit> findByRowIdOrderByDataEventoAscIdAsc(Long rowId);
}
