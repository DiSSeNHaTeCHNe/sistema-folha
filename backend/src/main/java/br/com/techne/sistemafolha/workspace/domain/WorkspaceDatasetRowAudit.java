package br.com.techne.sistemafolha.workspace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.util.Map;

@Data
@Entity
@NoArgsConstructor
@Table(name = "workspace_dataset_row_audit")
public class WorkspaceDatasetRowAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "row_id")
    private Long rowId;

    @Column(name = "autor_usuario_id", nullable = false)
    private Long autorUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DatasetRowAuditAction acao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_anteriores", columnDefinition = "jsonb")
    private Map<String, Object> valoresAnteriores;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valores_novos", columnDefinition = "jsonb")
    private Map<String, Object> valoresNovos;

    @Column(name = "data_evento", nullable = false, updatable = false)
    private LocalDateTime dataEvento;

    @PrePersist
    protected void onCreate() {
        if (dataEvento == null) {
            dataEvento = LocalDateTime.now(Clock.systemDefaultZone());
        }
    }
}
