package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.DatasetRowAuditAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Entrada de auditoria de linha de dataset")
public record DatasetRowAuditEntryDTO(
    Long id,
    Long rowId,
    Long autorUsuarioId,
    DatasetRowAuditAction acao,
    Map<String, Object> valoresAnteriores,
    Map<String, Object> valoresNovos,
    LocalDateTime dataEvento
) {}
