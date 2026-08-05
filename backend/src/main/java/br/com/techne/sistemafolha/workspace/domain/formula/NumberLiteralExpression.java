package br.com.techne.sistemafolha.workspace.domain.formula;

import java.math.BigDecimal;

public record NumberLiteralExpression(BigDecimal value) implements FormulaExpression {
}
