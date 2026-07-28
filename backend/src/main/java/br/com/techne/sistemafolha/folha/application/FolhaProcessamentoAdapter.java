package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.api.ProcessamentoOpcoes;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import br.com.techne.sistemafolha.folha.port.FolhaProcessamentoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FolhaProcessamentoAdapter implements FolhaProcessamentoPort {

    private final FolhaProcessamentoService folhaProcessamentoService;

    @Override
    @Transactional
    public ProcessamentoResultadoDTO processar(
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean decimoTerceiro,
            boolean recalcularFerias) {
        return folhaProcessamentoService.processar(
            competenciaInicio,
            competenciaFim,
            decimoTerceiro,
            new ProcessamentoOpcoes(recalcularFerias));
    }
}
