package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Template publicado com versão atual")
public record TemplateDTO(
    Long id,
    String nome,
    TemplateTipo tipo,
    Integer versaoAtual,
    String estruturaHash,
    LocalDateTime dataPublicacao,
    Long publicadorUsuarioId,
    boolean novaVersaoCriada
) {}
