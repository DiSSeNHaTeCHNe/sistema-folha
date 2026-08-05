package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceTemplateInstallationRepository extends JpaRepository<WorkspaceTemplateInstallation, Long> {

    List<WorkspaceTemplateInstallation> findByUsuarioIdOrderByDataInstalacaoDesc(Long usuarioId);

    Optional<WorkspaceTemplateInstallation> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<WorkspaceTemplateInstallation> findByUsuarioIdAndTemplateId(Long usuarioId, Long templateId);

    List<WorkspaceTemplateInstallation> findByUsuarioIdAndTemplateIdOrderByDataInstalacaoDesc(
        Long usuarioId, Long templateId);
}
