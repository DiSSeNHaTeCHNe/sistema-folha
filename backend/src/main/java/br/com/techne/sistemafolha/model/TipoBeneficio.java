package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@Table(name = "tipo_beneficio")
@EqualsAndHashCode(callSuper = false)
public class TipoBeneficio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O código é obrigatório")
    @Size(max = 50, message = "O código deve ter no máximo 50 caracteres")
    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 200, message = "A descrição deve ter no máximo 200 caracteres")
    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao", nullable = false)
    private LocalDateTime dataAtualizacao;

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
