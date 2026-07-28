package br.com.techne.sistemafolha.folha.port;

import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;

import java.time.LocalDate;

public interface FolhaProcessamentoPort {

    ProcessamentoResultadoDTO processar(
        LocalDate competenciaInicio,
        LocalDate competenciaFim,
        boolean decimoTerceiro,
        boolean recalcularFerias);
}
