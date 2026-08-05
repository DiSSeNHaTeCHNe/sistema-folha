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
import java.util.HashMap;
import java.util.Map;

@Data
@Entity
@NoArgsConstructor
@Table(name = "workspace_dataset_row")
public class WorkspaceDatasetRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> valores = new HashMap<>();

    @Column(name = "ordem")
    private Integer ordem;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(Clock.systemDefaultZone());
        dataAtualizacao = LocalDateTime.now(Clock.systemDefaultZone());
        if (valores == null) {
            valores = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now(Clock.systemDefaultZone());
    }
}
