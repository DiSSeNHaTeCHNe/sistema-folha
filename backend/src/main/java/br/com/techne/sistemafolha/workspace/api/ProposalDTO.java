package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.domain.ProposalPayload;
import br.com.techne.sistemafolha.workspace.domain.ProposalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Proposta IA pendente ou resolvida")
public record ProposalDTO(
    Long id,
    ProposalStatus status,
    ProposalPayload payload,
    Long solicitanteUsuarioId,
    LocalDateTime dataCriacao,
    LocalDateTime dataExpiracao,
    LocalDateTime dataResolucao
) {}
