package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item do catálogo de templates visíveis ao usuário")
public record TemplateCatalogItemDTO(
    Long id,
    String nome,
    TemplateTipo tipo,
    Integer versaoAtual,
    Integer versaoMaisRecente,
    boolean atualizacaoDisponivel,
    Long publicadorUsuarioId,
    Long installationId,
    Integer versaoInstalada
) {}
