package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para criação de Nó do Organograma")
public record NoOrganogramaCreateDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 1, max = 100, message = "O nome deve ter entre 1 e 100 caracteres")
    @Schema(description = "Nome do nó", example = "Diretoria Executiva", required = true)
    String nome,

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
    @Schema(description = "Descrição do nó", example = "Responsável pela direção executiva da empresa")
    String descricao,

    @Schema(description = "ID do nó pai (opcional)", example = "null")
    Long parentId,

    @Schema(description = "Posição do nó entre os irmãos (opcional, será calculada automaticamente)", example = "0")
    Integer posicao
) {} 