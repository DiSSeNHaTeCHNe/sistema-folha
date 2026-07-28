package br.com.techne.sistemafolha.folha.api;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProcessamentoRequestDTO(
    @NotNull LocalDate competenciaInicio,
    @NotNull LocalDate competenciaFim,
    boolean decimoTerceiro,
    ProcessamentoOpcoes opcoes
) {
    public ProcessamentoRequestDTO {
        if (opcoes == null) {
            opcoes = new ProcessamentoOpcoes(false);
        }
    }
}
