package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.ProposalStatus;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceIaProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkspaceIaProposalRepository extends JpaRepository<WorkspaceIaProposal, Long> {

    long countBySolicitanteUsuarioIdAndStatus(Long solicitanteUsuarioId, ProposalStatus status);

    Optional<WorkspaceIaProposal> findByIdAndSolicitanteUsuarioId(Long id, Long solicitanteUsuarioId);

    @Query(value = """
        SELECT * FROM workspace_ia_proposal
        WHERE solicitante_usuario_id = :usuarioId
          AND status = :status
          AND payload->>'dedupHash' = :dedupHash
        LIMIT 1
        """, nativeQuery = true)
    Optional<WorkspaceIaProposal> findPendingByDedupHash(
        @Param("usuarioId") Long usuarioId,
        @Param("status") String status,
        @Param("dedupHash") String dedupHash
    );

    List<WorkspaceIaProposal> findByStatusAndDataExpiracaoBefore(
        ProposalStatus status, LocalDateTime before
    );
}
