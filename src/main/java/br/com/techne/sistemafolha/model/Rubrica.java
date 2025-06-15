package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubricas")
public class Rubrica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRubrica tipo;

    @Column
    private Double porcentagem;

    @Column(nullable = false)
    private Boolean ativo = true;

    public enum TipoRubrica {
        PROVENTO,
        DESCONTO,
        INFORMATIVO
    }
} 