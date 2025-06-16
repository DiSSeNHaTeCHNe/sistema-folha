package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "cargos")
public class Cargo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 10, unique = true)
    private String codigo;
    
    @Column(nullable = false, length = 100)
    private String descricao;
    
    @Column(nullable = false)
    private Boolean ativo = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linha_negocio_id", nullable = false)
    private LinhaNegocio linhaNegocio;
} 