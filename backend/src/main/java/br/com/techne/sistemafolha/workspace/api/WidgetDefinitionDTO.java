package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(description = "Definição de widget do usuário")
public record WidgetDefinitionDTO(
    Long id,
    String nome,
    String tipo,
    List<WidgetSourceRef> fontes,
    String formula,
    Map<String, Object> config,
    boolean invalido
) {}
