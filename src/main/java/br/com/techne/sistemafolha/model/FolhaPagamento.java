package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "folha_pagamento")
public class FolhaPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @ManyToOne
    @JoinColumn(name = "rubrica_id", nullable = false)
    private Rubrica rubrica;

    @ManyToOne
    @JoinColumn(name = "cargo_id")
    private Cargo cargo;

    @ManyToOne
    @JoinColumn(name = "centro_custo_id")
    private CentroCusto centroCusto;

    @ManyToOne
    @JoinColumn(name = "linha_negocio_id")
    private LinhaNegocio linhaNegocio;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "quantidade", nullable = false)
    private BigDecimal quantidade;

    @Column(name = "base_calculo")
    private BigDecimal baseCalculo;

    @Column(nullable = false)
    private Boolean ativo = true;
} 