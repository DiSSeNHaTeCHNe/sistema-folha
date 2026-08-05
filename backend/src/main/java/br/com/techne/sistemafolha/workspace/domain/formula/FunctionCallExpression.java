package br.com.techne.sistemafolha.workspace.domain.formula;

import java.util.List;

public record FunctionCallExpression(
    String name,
    List<FormulaExpression> arguments
) implements FormulaExpression {
}
