package br.com.techne.sistemafolha.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "DTO para Nó do Organograma")
public record NoOrganogramaDTO(
    @Schema(description = "Identificador único do nó", example = "1")
    Long id,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 1, max = 100, message = "O nome deve ter entre 1 e 100 caracteres")
    @Schema(description = "Nome do nó", example = "Diretoria Executiva", required = true)
    String nome,

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres")
    @Schema(description = "Descrição do nó", example = "Responsável pela direção executiva da empresa")
    String descricao,

    @NotNull(message = "O nível é obrigatório")
    @Schema(description = "Nível hierárquico do nó (0 = raiz)", example = "0", required = true)
    Integer nivel,

    @Schema(description = "ID do nó pai", example = "null")
    Long parentId,

    @Schema(description = "Nome do nó pai", example = "Presidência")
    String parentNome,

    @NotNull(message = "A posição é obrigatória")
    @Schema(description = "Posição do nó entre os irmãos", example = "0", required = true)
    Integer posicao,

    @Schema(description = "Indica se o nó está ativo", example = "true")
    Boolean ativo,

    @Schema(description = "Indica se este nó pertence ao organograma ativo", example = "false")
    Boolean organogramaAtivo,

    @Schema(description = "Lista de IDs dos funcionários associados ao nó")
    List<Long> funcionarioIds,

    @Schema(description = "Lista de funcionários associados ao nó")
    List<FuncionarioDTO> funcionarios,

    @Schema(description = "Lista de IDs dos centros de custo associados ao nó")
    List<Long> centroCustoIds,

    @Schema(description = "Lista de centros de custo associados ao nó")
    List<CentroCustoDTO> centrosCusto,

    @Schema(description = "Lista de nós filhos")
    List<NoOrganogramaDTO> children,

    @Schema(description = "Data de criação", example = "2024-01-01T10:00:00")
    LocalDateTime dataCriacao,

    @Schema(description = "Data da última atualização", example = "2024-01-01T10:00:00")
    LocalDateTime dataAtualizacao,

    @Schema(description = "Usuário que criou o registro", example = "admin")
    String criadoPor,

    @Schema(description = "Usuário que atualizou o registro", example = "admin")
    String atualizadoPor
) {} 