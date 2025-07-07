package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumo_folha_pagamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumoFolhaPagamento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "total_empregados", nullable = false)
    private Integer totalEmpregados;
    
    @Column(name = "total_encargos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalEncargos;
    
    @Column(name = "total_pagamentos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPagamentos;
    
    @Column(name = "total_descontos", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDescontos;
    
    @Column(name = "total_liquido", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalLiquido;
    
    @Column(name = "competencia_inicio", nullable = false)
    private LocalDate competenciaInicio;
    
    @Column(name = "competencia_fim", nullable = false)
    private LocalDate competenciaFim;
    
    @Column(name = "data_importacao")
    private LocalDateTime dataImportacao;
    
    @Column(name = "ativo")
    private Boolean ativo = true;
} 