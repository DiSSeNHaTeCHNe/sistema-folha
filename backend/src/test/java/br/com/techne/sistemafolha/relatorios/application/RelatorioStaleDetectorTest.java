package br.com.techne.sistemafolha.relatorios.application;

import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioStaleDetectorTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Instant FIXED = Instant.parse("2026-06-15T12:00:00Z");

    private RelatorioGeracaoProperties properties;
    private RelatorioStaleDetector detector;

    @BeforeEach
    void setUp() {
        properties = new RelatorioGeracaoProperties();
        properties.setTimeoutSegundos(60);
        properties.setStaleGraceSegundos(120);
        Clock clock = Clock.fixed(FIXED, ZONE);
        detector = new RelatorioStaleDetector(properties, clock);
    }

    @Test
    void staleThreshold_retornaTimeoutMaisGrace() {
        assertEquals(Duration.ofSeconds(180), detector.staleThreshold());
    }

    @Test
    void isStale_pendenteSemBlobIdadeAcimaLimite_retornaTrue() {
        Relatorio relatorio = pendente(LocalDateTime.ofInstant(FIXED, ZONE).minusSeconds(181));
        assertTrue(detector.isStale(relatorio, false));
    }

    @Test
    void isStale_pendenteSemBlobIdadeAbaixoLimite_retornaFalse() {
        Relatorio relatorio = pendente(LocalDateTime.ofInstant(FIXED, ZONE).minusSeconds(179));
        assertFalse(detector.isStale(relatorio, false));
    }

    @Test
    void isStale_dataCriacaoNull_retornaTrueImediato() {
        Relatorio relatorio = pendente(null);
        assertTrue(detector.isStale(relatorio, false));
    }

    @Test
    void isStale_comBlobPdf_retornaFalse() {
        Relatorio relatorio = pendente(LocalDateTime.ofInstant(FIXED, ZONE).minusSeconds(500));
        assertFalse(detector.isStale(relatorio, true));
    }

    @Test
    void isStale_statusProcessado_retornaFalse() {
        Relatorio relatorio = pendente(LocalDateTime.ofInstant(FIXED, ZONE).minusSeconds(500));
        relatorio.setStatus(RelatorioStatus.PROCESSADO);
        assertFalse(detector.isStale(relatorio, false));
    }

    private Relatorio pendente(LocalDateTime dataCriacao) {
        Relatorio relatorio = new Relatorio();
        relatorio.setId(1L);
        relatorio.setTipo(RelatorioTipo.FOLHA);
        relatorio.setStatus(RelatorioStatus.PENDENTE);
        relatorio.setDataCriacao(dataCriacao);
        return relatorio;
    }
}
