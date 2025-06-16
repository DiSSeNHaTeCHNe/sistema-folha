package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Entity
@Table(name = "linhas_negocio")
public class LinhaNegocio {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 10)
    @Column(unique = true)
    private String codigo;

    @NotBlank
    @Size(max = 100)
    private String descricao;

    private boolean ativo = true;
} 