package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;

import java.time.LocalDateTime;
import java.util.List;

public record RelatorioFolhaModel(
    BrandingTheme branding,
    String competenciaLabel,
    String geradoPor,
    LocalDateTime geradoEm,
    DashboardStatsDTO stats,
    List<EvolucaoMensalDTO> evolucao6Meses,
    boolean semDados
) {}
