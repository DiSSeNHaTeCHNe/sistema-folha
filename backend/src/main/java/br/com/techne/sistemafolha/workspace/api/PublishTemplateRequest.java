package br.com.techne.sistemafolha.workspace.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

@Schema(description = "Publicar dataset ou widget definition como template (WKS-15)")
public record PublishTemplateRequest(
    @Schema(description = "ID do dataset salvo a publicar")
    Long datasetId,

    @Schema(description = "ID da widget definition salva a publicar")
    Long widgetDefinitionId
) {
    @AssertTrue(message = "Informe datasetId ou widgetDefinitionId, não ambos")
    public boolean isFonteUnica() {
        boolean hasDataset = datasetId != null;
        boolean hasWidget = widgetDefinitionId != null;
        return hasDataset ^ hasWidget;
    }
}
