package br.com.techne.sistemafolha.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "workspace_dataset")
public class WorkspaceDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 120)
    private String nome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema", nullable = false, columnDefinition = "jsonb")
    private List<DatasetFieldSchema> schema = new ArrayList<>();

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(Clock.systemDefaultZone());
        dataAtualizacao = LocalDateTime.now(Clock.systemDefaultZone());
        if (schema == null) {
            schema = new ArrayList<>();
        }
        if (schemaVersion == null) {
            schemaVersion = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now(Clock.systemDefaultZone());
    }
}
