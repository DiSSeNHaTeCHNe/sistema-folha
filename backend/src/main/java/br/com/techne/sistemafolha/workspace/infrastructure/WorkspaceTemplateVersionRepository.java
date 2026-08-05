package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceTemplateVersionRepository extends JpaRepository<WorkspaceTemplateVersion, Long> {

    List<WorkspaceTemplateVersion> findByTemplateIdOrderByVersaoDesc(Long templateId);

    Optional<WorkspaceTemplateVersion> findByTemplateIdAndVersao(Long templateId, Integer versao);

    Optional<WorkspaceTemplateVersion> findFirstByTemplateIdOrderByVersaoDesc(Long templateId);

    Optional<WorkspaceTemplateVersion> findByTemplateIdAndEstruturaHash(Long templateId, String estruturaHash);
}
