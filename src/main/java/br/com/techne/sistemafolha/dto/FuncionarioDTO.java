package br.com.techne.sistemafolha.dto;

import java.time.LocalDate;

public record FuncionarioDTO(
    Long id,
    String nome,
    String cargo,
    String centroCusto,
    String linhaNegocio,
    String idExterno,
    LocalDate dataAdmissao,
    String sexo,
    String tipoSalario,
    String funcao,
    Integer depIrrf,
    Integer depSalFamilia,
    String vinculo
) {} 