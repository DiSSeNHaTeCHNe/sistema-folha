package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import br.com.techne.sistemafolha.workspace.domain.formula.BinaryExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FieldReferenceExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FunctionCallExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.NumberLiteralExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaParseException;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaParser;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaTokenizer;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class FormulaEngine {

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
}
