package br.com.techne.sistemafolha.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "funcionarios")
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cargo;

    @Column(name = "centro_custo", nullable = false)
    private String centroCusto;

    @Column(name = "linha_negocio", nullable = false)
    private String linhaNegocio;

    @Column(name = "id_externo", nullable = false, unique = true)
    private String idExterno;

    @Column(name = "data_admissao")
    private LocalDate dataAdmissao;

    @Column
    private String sexo;

    @Column(name = "tipo_salario")
    private String tipoSalario;

    @Column
    private String funcao;

    @Column(name = "dep_irrf")
    private Integer depIrrf;

    @Column(name = "dep_sal_familia")
    private Integer depSalFamilia;

    @Column
    private String vinculo;

    @Column(nullable = false)
    private Boolean ativo = true;
} 