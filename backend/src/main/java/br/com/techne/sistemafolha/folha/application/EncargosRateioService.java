package br.com.techne.sistemafolha.folha.application;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rateio D4-CLT: encargos proporcionais ao bruto CLT por funcionário.
 * Callers scoped SHALL pass {@code totalEncargos = 0} (B1) — encargos permanecem zero.
 *
 * @deprecated Composição de {@code custoEmpresa} — use porcentagem de rubrica (AD-012 / fix2).
 *             Mantido para testes utilitários do algoritmo de rateio.
 */
@Service
@Deprecated(since = "fix2", forRemoval = false)
public class EncargosRateioService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal TOLERANCIA_SOMA = new BigDecimal("0.01");

    public Map<Long, BigDecimal> ratearPorFuncionario(
            Map<Long, BigDecimal> brutoPorFuncionario, BigDecimal totalEncargos) {
        if (brutoPorFuncionario == null || brutoPorFuncionario.isEmpty()) {
            return Collections.emptyMap();
        }
        if (totalEncargos == null || totalEncargos.compareTo(BigDecimal.ZERO) == 0) {
            Map<Long, BigDecimal> zeros = new HashMap<>();
            brutoPorFuncionario.keySet().forEach(id -> zeros.put(id, BigDecimal.ZERO.setScale(SCALE, ROUNDING)));
            return zeros;
        }

        BigDecimal totalBruto = brutoPorFuncionario.values().stream()
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalBruto.compareTo(BigDecimal.ZERO) == 0) {
            Map<Long, BigDecimal> zeros = new HashMap<>();
            brutoPorFuncionario.keySet().forEach(id -> zeros.put(id, BigDecimal.ZERO.setScale(SCALE, ROUNDING)));
            return zeros;
        }

        Map<Long, BigDecimal> rateio = new LinkedHashMap<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        Long ultimoId = null;

        for (Map.Entry<Long, BigDecimal> entry : brutoPorFuncionario.entrySet()) {
            ultimoId = entry.getKey();
            BigDecimal bruto = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            BigDecimal parcela = totalEncargos
                .multiply(bruto)
                .divide(totalBruto, SCALE, ROUNDING);
            rateio.put(entry.getKey(), parcela);
            acumulado = acumulado.add(parcela);
        }

        if (ultimoId != null) {
            BigDecimal diferenca = totalEncargos.subtract(acumulado);
            if (diferenca.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal ajustada = rateio.get(ultimoId).add(diferenca);
                rateio.put(ultimoId, ajustada.setScale(SCALE, ROUNDING));
            }
        }

        return rateio;
    }

    public BigDecimal rateioParaFuncionario(
            Long funcionarioId, Map<Long, BigDecimal> brutoPorFuncionario, BigDecimal totalEncargos) {
        return ratearPorFuncionario(brutoPorFuncionario, totalEncargos)
            .getOrDefault(funcionarioId, BigDecimal.ZERO.setScale(SCALE, ROUNDING));
    }

    static boolean somaDentroDaTolerancia(BigDecimal totalEncargos, Map<Long, BigDecimal> rateio) {
        BigDecimal soma = rateio.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.subtract(totalEncargos).abs().compareTo(TOLERANCIA_SOMA) <= 0;
    }
}
