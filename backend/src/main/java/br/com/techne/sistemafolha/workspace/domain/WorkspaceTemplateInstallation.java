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
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@NoArgsConstructor
@Table(name = "workspace_template_installation")
public class WorkspaceTemplateInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "versao_instalada", nullable = false)
    private Integer versaoInstalada;

    @Column(name = "workspace_id")
    private Long workspaceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dataset_ids", columnDefinition = "jsonb")
    private Map<String, Long> datasetIds = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "widget_definition_ids", columnDefinition = "jsonb")
    private Map<String, Long> widgetDefinitionIds = new HashMap<>();

    @Column(name = "data_instalacao", nullable = false, updatable = false)
    private LocalDateTime dataInstalacao;

    @PrePersist
    protected void onCreate() {
        if (dataInstalacao == null) {
            dataInstalacao = LocalDateTime.now(Clock.systemDefaultZone());
        }
        if (datasetIds == null) {
            datasetIds = new HashMap<>();
        }
        if (widgetDefinitionIds == null) {
            widgetDefinitionIds = new HashMap<>();
        }
    }
}
