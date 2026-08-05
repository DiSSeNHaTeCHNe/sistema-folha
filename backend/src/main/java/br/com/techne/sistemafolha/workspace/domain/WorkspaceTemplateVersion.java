package br.com.techne.sistemafolha.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "workspace_template_version")
public class WorkspaceTemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(nullable = false)
    private Integer versao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "estrutura", nullable = false, columnDefinition = "jsonb")
    private TemplateStructurePayload estrutura;

    @Column(name = "estrutura_hash", nullable = false, length = 64)
    private String estruturaHash;

    @Column(name = "data_publicacao", nullable = false, updatable = false)
    private LocalDateTime dataPublicacao;

    @PrePersist
    protected void onCreate() {
        if (dataPublicacao == null) {
            dataPublicacao = LocalDateTime.now(Clock.systemDefaultZone());
        }
    }
}
