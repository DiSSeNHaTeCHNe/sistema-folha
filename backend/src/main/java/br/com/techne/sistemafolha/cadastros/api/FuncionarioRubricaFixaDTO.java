package br.com.techne.sistemafolha.cadastros.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FuncionarioRubricaFixaDTO(
    Long id,
    @Schema(description = "ID do funcionário; omitir/null = fixa global")
    Long funcionarioId,
    @NotNull(message = "A rubrica é obrigatória")
    @Schema(description = "ID da rubrica") Long rubricaId,
    @Schema(description = "Valor fixo (obrigatório se rubrica não for calculada)") BigDecimal valor,
    @NotNull(message = "A vigência de início é obrigatória")
    @Schema(description = "Início da vigência") LocalDate vigenciaInicio,
    @Schema(description = "Fim da vigência (null = aberta)") LocalDate vigenciaFim,
    @Schema(description = "Comentário opcional") String comentario,
    @Schema(description = "Ativo") Boolean ativo,
    @Schema(readOnly = true, description = "Nome do funcionário; null para fixa global")
    String funcionarioNome,
    @Schema(readOnly = true, description = "Código da rubrica (somente leitura)") String rubricaCodigo,
    @Schema(readOnly = true, description = "Descrição da rubrica (somente leitura)") String rubricaDescricao,
    @Schema(readOnly = true, description = "Live rubricas.porcentagem; default 100 se null")
    Double porcentagem
) {}
