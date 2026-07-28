package br.com.techne.sistemafolha.cadastros.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuncionarioRubricaFixaDTO(
    Long id,
    @NotNull(message = "O funcionário é obrigatório")
    @Schema(description = "ID do funcionário") Long funcionarioId,
    @NotNull(message = "A rubrica é obrigatória")
    @Schema(description = "ID da rubrica") Long rubricaId,
    @Schema(description = "Valor fixo (obrigatório se rubrica não for calculada)") BigDecimal valor,
    @NotNull(message = "A vigência de início é obrigatória")
    @Schema(description = "Início da vigência") LocalDate vigenciaInicio,
    @Schema(description = "Fim da vigência (null = aberta)") LocalDate vigenciaFim,
    @Schema(description = "Comentário opcional") String comentario,
    @Schema(description = "Ativo") Boolean ativo,
    @Schema(description = "Nome do funcionário (somente leitura)") String funcionarioNome,
    @Schema(description = "Código da rubrica (somente leitura)") String rubricaCodigo,
    @Schema(description = "Descrição da rubrica (somente leitura)") String rubricaDescricao
) {}
