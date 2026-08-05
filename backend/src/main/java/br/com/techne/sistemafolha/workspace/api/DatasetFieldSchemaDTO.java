package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.ReferenciaEntidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Definição de campo do dataset")
public record DatasetFieldSchemaDTO(
    @NotBlank @Size(max = 120) String nome,
    @NotNull DatasetFieldType tipo,
    ReferenciaEntidade referenciaEntidade,
    Boolean obrigatorio,
    @Size(max = 500) String observacao
) {}
