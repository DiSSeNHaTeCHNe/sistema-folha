package br.com.techne.sistemafolha.folha.domain;

import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ficha_linha")
public class FichaLinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ficha_mensal_id", nullable = false)
    private FichaMensal fichaMensal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubrica_id", nullable = false)
    private Rubrica rubrica;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_linha", nullable = false, columnDefinition = "origem_linha")
    private OrigemLinha origemLinha;

    @Column(name = "operador_bruto", nullable = false)
    private Short operadorBruto;

    @Column(name = "operador_liquido", nullable = false)
    private Short operadorLiquido;

    @Column(name = "operador_custo", nullable = false)
    private Short operadorCusto;

    @Column(nullable = false)
    private Boolean ativo = true;
}
