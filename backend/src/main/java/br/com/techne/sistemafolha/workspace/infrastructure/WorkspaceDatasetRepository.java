package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceDatasetRepository extends JpaRepository<WorkspaceDataset, Long> {

    List<WorkspaceDataset> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    Optional<WorkspaceDataset> findByUsuarioIdAndId(Long usuarioId, Long id);

    long countByUsuarioId(Long usuarioId);
}
