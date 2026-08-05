package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Requisição para criar proposta IA")
public record CreateProposalRequest(
    @NotBlank String tipo,
    @Size(max = 2000) String descricaoNatural
) {}
