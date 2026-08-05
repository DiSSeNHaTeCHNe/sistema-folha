package br.com.techne.sistemafolha.workspace.domain.formula;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EvaluationContext {

    private final Map<String, List<BigDecimal>> fieldSeries;

    private EvaluationContext(Map<String, List<BigDecimal>> fieldSeries) {
        this.fieldSeries = Collections.unmodifiableMap(fieldSeries);
    }

    public List<BigDecimal> series(String fieldName) {
        return fieldSeries.getOrDefault(fieldName, List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, List<BigDecimal>> fieldSeries = new HashMap<>();

        public Builder putSeries(String fieldName, List<BigDecimal> values) {
            fieldSeries.put(fieldName, values);
            return this;
        }

        public EvaluationContext build() {
            return new EvaluationContext(fieldSeries);
        }
    }
}
