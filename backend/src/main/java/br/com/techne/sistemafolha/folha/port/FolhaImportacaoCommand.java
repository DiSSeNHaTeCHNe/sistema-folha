package br.com.techne.sistemafolha.folha.port;

import java.time.LocalDate;
import java.util.List;

public record FolhaImportacaoCommand(
    LocalDate competenciaInicio,
    LocalDate competenciaFim,
    boolean decimoTerceiro,
    boolean substituirExistente,
    List<FolhaImportacaoLinhaCommand> linhas,
    FolhaImportacaoResumoCommand resumo
) {}
