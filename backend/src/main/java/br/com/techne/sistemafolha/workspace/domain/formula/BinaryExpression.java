package br.com.techne.sistemafolha.workspace.domain.formula;

public record BinaryExpression(
    ComparisonOperator operator,
    FormulaExpression left,
    FormulaExpression right
) implements FormulaExpression {
}
