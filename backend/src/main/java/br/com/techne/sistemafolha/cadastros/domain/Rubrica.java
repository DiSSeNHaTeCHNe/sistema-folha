package br.com.techne.sistemafolha.cadastros.domain;

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

    @ManyToOne
    @JoinColumn(name = "tipo_rubrica_id", nullable = false)
    private TipoRubrica tipoRubrica;

    @Column
    private Double porcentagem;

    @Column(name = "operador_bruto", nullable = false)
    private Short operadorBruto = 1;

    @Column(name = "operador_liquido", nullable = false)
    private Short operadorLiquido = 1;

    @Column(name = "operador_custo", nullable = false)
    private Short operadorCusto = 1;

    @Column(nullable = false)
    private Boolean ativo = true;
} 