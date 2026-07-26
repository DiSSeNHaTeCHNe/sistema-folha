package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "beneficio_mensal")
@EqualsAndHashCode(callSuper = false)
public class BeneficioMensal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O funcionário é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @NotNull(message = "O tipo de benefício é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_beneficio_id", nullable = false)
    private TipoBeneficio tipoBeneficio;

    @NotNull(message = "O valor é obrigatório")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @NotNull(message = "A competência de início é obrigatória")
    @Column(name = "competencia_inicio", nullable = false)
    private LocalDate competenciaInicio;

    @NotNull(message = "A competência de fim é obrigatória")
    @Column(name = "competencia_fim", nullable = false)
    private LocalDate competenciaFim;

    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
    @Column(length = 500)
    private String observacao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @Column(name = "atualizado_por", length = 100)
    private String atualizadoPor;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
