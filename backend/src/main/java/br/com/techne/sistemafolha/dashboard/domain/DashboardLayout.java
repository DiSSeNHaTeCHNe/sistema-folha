package br.com.techne.sistemafolha.dashboard.domain;

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
@Table(name = "dashboard_layout")
public class DashboardLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String nome = "Meu dashboard";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "widgets", nullable = false, columnDefinition = "jsonb")
    private List<WidgetInstancePayload> widgets = new ArrayList<>();

    @Column(name = "versao_schema", nullable = false)
    private Integer versaoSchema = 1;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now(Clock.systemDefaultZone());
        dataAtualizacao = LocalDateTime.now(Clock.systemDefaultZone());
        if (nome == null) {
            nome = "Meu dashboard";
        }
        if (widgets == null) {
            widgets = new ArrayList<>();
        }
        if (versaoSchema == null) {
            versaoSchema = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now(Clock.systemDefaultZone());
    }
}
