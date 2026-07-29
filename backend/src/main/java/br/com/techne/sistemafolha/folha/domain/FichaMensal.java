package br.com.techne.sistemafolha.folha.domain;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ficha_mensal")
public class FichaMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @Column(name = "competencia_inicio", nullable = false)
    private LocalDate competenciaInicio;

    @Column(name = "competencia_fim", nullable = false)
    private LocalDate competenciaFim;

    @Column(name = "decimo_terceiro", nullable = false)
    private Boolean decimoTerceiro = false;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal bruto = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal liquido = BigDecimal.ZERO;

    @Column(name = "custo_folha", nullable = false, precision = 15, scale = 2)
    private BigDecimal custoFolha = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean ativo = true;
}
