package br.com.techne.sistemafolha.folha.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncargosRateioServiceTest {

    private final EncargosRateioService service = new EncargosRateioService();

    @Test
    void ratearPorFuncionario_8kE2kBruto_1kEncargos_retorna800E200() {
        Map<Long, BigDecimal> bruto = new LinkedHashMap<>();
        bruto.put(1L, new BigDecimal("8000.00"));
        bruto.put(2L, new BigDecimal("2000.00"));

        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, new BigDecimal("1000.00"));

        assertEquals(0, new BigDecimal("800.00").compareTo(rateio.get(1L)));
        assertEquals(0, new BigDecimal("200.00").compareTo(rateio.get(2L)));
        assertTrue(EncargosRateioService.somaDentroDaTolerancia(new BigDecimal("1000.00"), rateio));
    }

    @Test
    void ratearPorFuncionario_totalEncargosZero_retornaZeros() {
        Map<Long, BigDecimal> bruto = Map.of(1L, new BigDecimal("5000.00"));

        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, BigDecimal.ZERO);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(rateio.get(1L)));
    }

    @Test
    void ratearPorFuncionario_brutoZero_retornaZeros() {
        Map<Long, BigDecimal> bruto = Map.of(1L, BigDecimal.ZERO);

        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, new BigDecimal("1000.00"));

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(rateio.get(1L)));
    }

    @Test
    void ratearPorFuncionario_tresFuncionarios_ajustaCentavosNaUltimaParcela() {
        Map<Long, BigDecimal> bruto = new LinkedHashMap<>();
        bruto.put(1L, new BigDecimal("3333.33"));
        bruto.put(2L, new BigDecimal("3333.33"));
        bruto.put(3L, new BigDecimal("3333.34"));

        BigDecimal totalEncargos = new BigDecimal("1000.00");
        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, totalEncargos);

        assertTrue(EncargosRateioService.somaDentroDaTolerancia(totalEncargos, rateio));
    }

    @Test
    void rateioParaFuncionario_delegaParaRatear() {
        Map<Long, BigDecimal> bruto = Map.of(10L, new BigDecimal("8000.00"), 20L, new BigDecimal("2000.00"));

        BigDecimal parcela = service.rateioParaFuncionario(10L, bruto, new BigDecimal("1000.00"));

        assertEquals(0, new BigDecimal("800.00").compareTo(parcela));
    }

    @Test
    void ratearPorFuncionario_mapaNullOuVazio_retornaVazio() {
        assertTrue(service.ratearPorFuncionario(null, new BigDecimal("100.00")).isEmpty());
        assertTrue(service.ratearPorFuncionario(Map.of(), new BigDecimal("100.00")).isEmpty());
    }

    @Test
    void ratearPorFuncionario_totalEncargosNull_retornaZeros() {
        Map<Long, BigDecimal> bruto = Map.of(1L, new BigDecimal("5000.00"));

        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, null);

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(rateio.get(1L)));
    }

    @Test
    void ratearPorFuncionario_brutoNullNoMapa_trataComoZero() {
        Map<Long, BigDecimal> bruto = new LinkedHashMap<>();
        bruto.put(1L, null);
        bruto.put(2L, new BigDecimal("1000.00"));

        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, new BigDecimal("100.00"));

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(rateio.get(1L)));
        assertEquals(0, new BigDecimal("100.00").compareTo(rateio.get(2L)));
    }

    @Test
    void ratearPorFuncionario_ajustaCentavosQuandoSomaDiverge() {
        Map<Long, BigDecimal> bruto = new LinkedHashMap<>();
        bruto.put(1L, new BigDecimal("1.00"));
        bruto.put(2L, new BigDecimal("1.00"));
        bruto.put(3L, new BigDecimal("1.00"));

        Map<Long, BigDecimal> rateio = service.ratearPorFuncionario(bruto, new BigDecimal("1.00"));

        assertTrue(EncargosRateioService.somaDentroDaTolerancia(new BigDecimal("1.00"), rateio));
    }

    @Test
    void rateioParaFuncionario_idInexistente_retornaZero() {
        Map<Long, BigDecimal> bruto = Map.of(1L, new BigDecimal("5000.00"));

        BigDecimal parcela = service.rateioParaFuncionario(99L, bruto, new BigDecimal("100.00"));

        assertEquals(0, BigDecimal.ZERO.setScale(2).compareTo(parcela));
    }

    @Test
    void somaDentroDaTolerancia_foraDaTolerancia_retornaFalse() {
        Map<Long, BigDecimal> rateio = Map.of(1L, new BigDecimal("100.00"));

        assertFalse(EncargosRateioService.somaDentroDaTolerancia(new BigDecimal("200.00"), rateio));
    }
}
