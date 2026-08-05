package br.com.techne.sistemafolha.dashboard.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DashboardLayoutDTO(
    Long id,
    @Size(max = 100) String nome,
    @Size(max = 30) List<@Valid WidgetInstanceDTO> widgets
) {}
