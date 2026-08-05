package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Descritor de campo de fonte sistema para IA")
public record SystemFieldDescriptorDTO(
    String fonte,
    String nome,
    String tipo
) {}
