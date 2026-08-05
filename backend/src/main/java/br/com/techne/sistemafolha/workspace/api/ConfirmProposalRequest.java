package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

@Schema(description = "Ajustes opcionais ao confirmar proposta")
public record ConfirmProposalRequest(
    @Size(max = 120) String nome,
    List<@Valid DatasetFieldSchemaDTO> campos,
    @Size(max = 120) String tipoWidget,
    List<@Valid WidgetSourceRef> fontes,
    String formula,
    Map<String, Object> config,
    Long workspaceId
) {}
