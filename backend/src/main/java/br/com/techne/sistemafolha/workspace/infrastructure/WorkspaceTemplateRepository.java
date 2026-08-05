package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceTemplateRepository extends JpaRepository<WorkspaceTemplate, Long> {

    List<WorkspaceTemplate> findByAtivoTrueOrderByNomeAsc();

    Optional<WorkspaceTemplate> findByIdAndAtivoTrue(Long id);

    List<WorkspaceTemplate> findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(Long publicadorUsuarioId);
}
