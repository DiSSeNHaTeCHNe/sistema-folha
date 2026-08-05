package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceWidgetDefinitionRepository extends JpaRepository<WorkspaceWidgetDefinition, Long> {

    List<WorkspaceWidgetDefinition> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    Optional<WorkspaceWidgetDefinition> findByUsuarioIdAndId(Long usuarioId, Long id);

    long countByUsuarioId(Long usuarioId);
}
