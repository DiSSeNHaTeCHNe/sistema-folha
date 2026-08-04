package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class RelatorioStaleDetector {

    private final RelatorioGeracaoProperties properties;
    private final Clock clock;

    public RelatorioStaleDetector(RelatorioGeracaoProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public Duration staleThreshold() {
        return Duration.ofSeconds(properties.getTimeoutSegundos() + properties.getStaleGraceSegundos());
    }

    public boolean isStale(Relatorio relatorio, boolean hasPdfBlob) {
        if (relatorio.getStatus() != RelatorioStatus.PENDENTE || hasPdfBlob) {
            return false;
        }
        LocalDateTime dataCriacao = relatorio.getDataCriacao();
        if (dataCriacao == null) {
            return true;
        }
        LocalDateTime limite = dataCriacao.plus(staleThreshold());
        return LocalDateTime.now(clock).isAfter(limite);
    }
}
