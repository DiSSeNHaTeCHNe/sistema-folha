package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.DatasetRowAuditAction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Entrada da timeline agregada de auditoria do dataset")
public record DatasetAuditTimelineEntryDTO(
    Long rowId,
    DatasetRowAuditAction acao,
    Long autorUsuarioId,
    LocalDateTime dataEvento,
    String resumo
) {}
