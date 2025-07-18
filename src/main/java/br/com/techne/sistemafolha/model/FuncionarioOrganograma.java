package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "funcionario_organograma")
@EqualsAndHashCode(callSuper = false)
public class FuncionarioOrganograma {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "no_organograma_id", nullable = false)
    private NoOrganograma noOrganograma;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "criado_por", length = 100)
    private String criadoPor;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
} 