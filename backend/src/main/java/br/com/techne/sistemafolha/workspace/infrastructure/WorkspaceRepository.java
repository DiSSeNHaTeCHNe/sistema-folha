package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

    Optional<Workspace> findByUsuarioIdAndId(Long usuarioId, Long id);

    long countByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndNome(Long usuarioId, String nome);
}
