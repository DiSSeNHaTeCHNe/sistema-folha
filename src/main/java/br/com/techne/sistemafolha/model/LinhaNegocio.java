package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "linhas_negocio")
public class LinhaNegocio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String descricao;
    
    @Column(nullable = false)
    private Boolean ativo = true;
} 