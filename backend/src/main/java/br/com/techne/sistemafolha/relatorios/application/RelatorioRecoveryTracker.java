package br.com.techne.sistemafolha.relatorios.application;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class RelatorioRecoveryTracker {

    private final ConcurrentHashMap<Long, Boolean> attempted = new ConcurrentHashMap<>();

    public boolean hasAttempted(Long relatorioId) {
        return attempted.containsKey(relatorioId);
    }

    public void markAttempted(Long relatorioId) {
        attempted.put(relatorioId, Boolean.TRUE);
    }

    public void clear(Long relatorioId) {
        attempted.remove(relatorioId);
    }
}
