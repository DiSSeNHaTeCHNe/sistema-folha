package br.com.techne.sistemafolha.dashboard.port;

import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.EvolucaoMensalDTO;

import java.time.LocalDate;
import java.util.List;

public interface DashboardConsultaPort {

    DashboardStatsDTO getStatsForCompetencia(
        String login, LocalDate competenciaInicio, LocalDate competenciaFim, boolean decimoTerceiro);

    List<EvolucaoMensalDTO> getEvolucaoMeses(
        String login, LocalDate fimInclusive, int quantidadeMeses, boolean decimoTerceiro);
}
