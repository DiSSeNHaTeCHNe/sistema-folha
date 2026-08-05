package br.com.techne.sistemafolha.workspace.domain.formula;

import java.math.BigDecimal;

public record TypedValue(
    BigDecimal numericValue,
    boolean booleanValue,
    ValueKind kind
) {
    public enum ValueKind {
        NUMBER,
        BOOLEAN
    }

    public static TypedValue number(BigDecimal value) {
        return new TypedValue(value, false, ValueKind.NUMBER);
    }

    public static TypedValue bool(boolean value) {
        return new TypedValue(value ? BigDecimal.ONE : BigDecimal.ZERO, value, ValueKind.BOOLEAN);
    }

    public BigDecimal asNumber() {
        if (kind != ValueKind.NUMBER) {
            throw new IllegalStateException("Valor não numérico");
        }
        return numericValue;
    }

    public boolean asBoolean() {
        if (kind != ValueKind.BOOLEAN) {
            throw new IllegalStateException("Valor não booleano");
        }
        return booleanValue;
    }
}
