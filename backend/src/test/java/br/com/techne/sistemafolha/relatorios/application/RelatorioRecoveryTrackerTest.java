package br.com.techne.sistemafolha.relatorios.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelatorioRecoveryTrackerTest {

    private RelatorioRecoveryTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new RelatorioRecoveryTracker();
    }

    @Test
    void hasAttempted_inicialmenteFalse() {
        assertFalse(tracker.hasAttempted(42L));
    }

    @Test
    void markAttempted_registraTentativa() {
        tracker.markAttempted(42L);
        assertTrue(tracker.hasAttempted(42L));
    }

    @Test
    void clear_removeTentativa() {
        tracker.markAttempted(42L);
        tracker.clear(42L);
        assertFalse(tracker.hasAttempted(42L));
    }

    @Test
    void markAttempted_idsIndependentes() {
        tracker.markAttempted(1L);
        assertTrue(tracker.hasAttempted(1L));
        assertFalse(tracker.hasAttempted(2L));
    }

    @Test
    void operacoes_concorrentes_saoThreadSafe() throws InterruptedException {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicBoolean inconsistent = new AtomicBoolean(false);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int j = 0; j < 100; j++) {
                        tracker.markAttempted(99L);
                        tracker.hasAttempted(99L);
                        tracker.clear(99L);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    inconsistent.set(true);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        pool.shutdown();

        assertFalse(inconsistent.get());
    }
}
