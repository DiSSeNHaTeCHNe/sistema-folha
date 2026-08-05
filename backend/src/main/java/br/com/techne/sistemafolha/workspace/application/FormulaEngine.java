package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import br.com.techne.sistemafolha.workspace.domain.formula.BinaryExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.ComparisonOperator;
import br.com.techne.sistemafolha.workspace.domain.formula.EvaluationContext;
import br.com.techne.sistemafolha.workspace.domain.formula.FieldReferenceExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FunctionCallExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.NumberLiteralExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaParseException;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaParser;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaTokenizer;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaValidationResult;
import br.com.techne.sistemafolha.workspace.domain.formula.TypedValue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class FormulaEngine {

    private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

    private static final Set<String> ALLOWED_FUNCTIONS = Set.of(
        "SOMA", "MÉDIA", "MÍN", "MÁX", "CONTAGEM", "SE"
    );

    private static final Set<String> FORBIDDEN_IDENTIFIERS = Set.of(
        "RUNTIME", "EVAL"
    );

    private static final Set<String> AGGREGATION_FUNCTIONS = Set.of(
        "SOMA", "MÉDIA", "MÍN", "MÁX", "CONTAGEM"
    );

    private final FormulaTokenizer tokenizer = new FormulaTokenizer();

    public FormulaValidationResult validate(String expression, List<AvailableField> availableFields) {
        if (expression == null || expression.isBlank()) {
            return FormulaValidationResult.invalid("Expressão vazia");
        }
        try {
            FormulaExpression ast = parse(expression);
            Set<String> allowedFieldNames = new HashSet<>();
            for (AvailableField field : availableFields) {
                allowedFieldNames.add(field.name());
            }
            List<String> errors = new ArrayList<>();
            validateNode(ast, allowedFieldNames, errors);
            if (errors.isEmpty()) {
                return FormulaValidationResult.ok();
            }
            return FormulaValidationResult.invalid(errors);
        } catch (FormulaParseException ex) {
            return FormulaValidationResult.invalid(ex.getMessage());
        }
    }

    public TypedValue evaluate(String expression, EvaluationContext context) {
        FormulaExpression ast = parse(expression);
        return evaluateNode(ast, context);
    }

    public FormulaExpression parse(String expression) {
        List<br.com.techne.sistemafolha.workspace.domain.formula.Token> tokens = tokenizer.tokenize(expression);
        return new FormulaParser(tokens).parse();
    }

    private void validateNode(FormulaExpression node, Set<String> allowedFields, List<String> errors) {
        if (node instanceof NumberLiteralExpression) {
            return;
        }
        if (node instanceof FieldReferenceExpression field) {
            validateFieldReference(field.name(), allowedFields, errors);
            return;
        }
        if (node instanceof BinaryExpression binary) {
            validateNode(binary.left(), allowedFields, errors);
            validateNode(binary.right(), allowedFields, errors);
            return;
        }
        if (node instanceof FunctionCallExpression call) {
            validateFunction(call, allowedFields, errors);
        }
    }

    private void validateFunction(FunctionCallExpression call, Set<String> allowedFields, List<String> errors) {
        String fn = call.name();
        if (isForbidden(fn)) {
            errors.add("Identificador não permitido: " + fn);
            return;
        }
        if (!ALLOWED_FUNCTIONS.contains(fn)) {
            errors.add("Função não permitida: " + fn);
            return;
        }
        if ("SE".equals(fn)) {
            if (call.arguments().size() != 3) {
                errors.add("SE requer exatamente 3 argumentos");
            }
            call.arguments().forEach(arg -> validateNode(arg, allowedFields, errors));
            return;
        }
        if (AGGREGATION_FUNCTIONS.contains(fn)) {
            if (call.arguments().size() != 1) {
                errors.add(fn + " requer exatamente 1 argumento");
                return;
            }
            FormulaExpression arg = call.arguments().get(0);
            if (!(arg instanceof FieldReferenceExpression field)) {
                errors.add(fn + " requer referência a campo");
            } else {
                validateFieldReference(field.name(), allowedFields, errors);
            }
        }
    }

    private void validateFieldReference(String name, Set<String> allowedFields, List<String> errors) {
        if (isForbidden(name)) {
            errors.add("Identificador não permitido: " + name);
            return;
        }
        if (!allowedFields.contains(name)) {
            errors.add("Campo inválido: " + name);
        }
    }

    private boolean isForbidden(String identifier) {
        return FORBIDDEN_IDENTIFIERS.contains(identifier.toUpperCase(Locale.ROOT));
    }

    private TypedValue evaluateNode(FormulaExpression node, EvaluationContext context) {
        if (node instanceof NumberLiteralExpression number) {
            return TypedValue.number(number.value());
        }
        if (node instanceof FieldReferenceExpression field) {
            return TypedValue.number(aggregateSeries("SOMA", field.name(), context));
        }
        if (node instanceof BinaryExpression binary) {
            return evaluateBinary(binary, context);
        }
        if (node instanceof FunctionCallExpression call) {
            return evaluateFunction(call, context);
        }
        throw new FormulaParseException("Expressão inválida");
    }

    private TypedValue evaluateBinary(BinaryExpression binary, EvaluationContext context) {
        ComparisonOperator op = binary.operator();
        if (op == ComparisonOperator.GT || op == ComparisonOperator.LT || op == ComparisonOperator.GTE
            || op == ComparisonOperator.LTE || op == ComparisonOperator.EQ || op == ComparisonOperator.NEQ) {
            BigDecimal left = evaluateNumeric(binary.left(), context);
            BigDecimal right = evaluateNumeric(binary.right(), context);
            boolean result = switch (op) {
                case GT -> left.compareTo(right) > 0;
                case LT -> left.compareTo(right) < 0;
                case GTE -> left.compareTo(right) >= 0;
                case LTE -> left.compareTo(right) <= 0;
                case EQ -> left.compareTo(right) == 0;
                case NEQ -> left.compareTo(right) != 0;
                default -> throw new IllegalStateException("Operador não comparável");
            };
            return TypedValue.bool(result);
        }
        BigDecimal left = evaluateNumeric(binary.left(), context);
        BigDecimal right = evaluateNumeric(binary.right(), context);
        BigDecimal result = switch (op) {
            case ADD -> left.add(right, MATH_CONTEXT);
            case SUBTRACT -> left.subtract(right, MATH_CONTEXT);
            case MULTIPLY -> left.multiply(right, MATH_CONTEXT);
            case DIVIDE -> divideSafe(left, right);
            default -> throw new IllegalStateException("Operador aritmético inválido");
        };
        return TypedValue.number(result);
    }

    private TypedValue evaluateFunction(FunctionCallExpression call, EvaluationContext context) {
        return switch (call.name()) {
            case "SOMA", "MÉDIA", "MÍN", "MÁX", "CONTAGEM" -> {
                FieldReferenceExpression field = (FieldReferenceExpression) call.arguments().get(0);
                yield TypedValue.number(aggregateSeries(call.name(), field.name(), context));
            }
            case "SE" -> {
                boolean condition = evaluateNode(call.arguments().get(0), context).asBoolean();
                FormulaExpression branch = condition ? call.arguments().get(1) : call.arguments().get(2);
                yield evaluateNode(branch, context);
            }
            default -> throw new FormulaParseException("Função não permitida: " + call.name());
        };
    }

    private BigDecimal evaluateNumeric(FormulaExpression node, EvaluationContext context) {
        TypedValue value = evaluateNode(node, context);
        return value.asNumber();
    }

    private BigDecimal aggregateSeries(String function, String fieldName, EvaluationContext context) {
        List<BigDecimal> series = context.series(fieldName);
        if (series.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return switch (function) {
            case "SOMA" -> series.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "MÉDIA" -> series.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(series.size()), MATH_CONTEXT);
            case "MÍN" -> series.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case "MÁX" -> series.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
            case "CONTAGEM" -> BigDecimal.valueOf(series.size());
            default -> throw new IllegalStateException("Função de agregação inválida");
        };
    }

    private BigDecimal divideSafe(BigDecimal left, BigDecimal right) {
        if (right.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return left.divide(right, MATH_CONTEXT);
    }
}
