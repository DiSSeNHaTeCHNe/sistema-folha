package br.com.techne.sistemafolha.workspace.domain.formula;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import br.com.techne.sistemafolha.workspace.domain.formula.BinaryExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.ComparisonOperator;
import br.com.techne.sistemafolha.workspace.domain.formula.FieldReferenceExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.FunctionCallExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.NumberLiteralExpression;
import br.com.techne.sistemafolha.workspace.domain.formula.Token;
import br.com.techne.sistemafolha.workspace.domain.formula.TokenType;

public class FormulaParser {

    private final List<Token> tokens;
    private int current = 0;

    public FormulaParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public FormulaExpression parse() {
        FormulaExpression expr = parseComparison();
        if (peek().type() != TokenType.EOF) {
            throw new FormulaParseException("Expressão inválida", peek().position());
        }
        return expr;
    }

    private FormulaExpression parseComparison() {
        FormulaExpression left = parseAdditive();
        while (isComparisonOperator(peek().type())) {
            ComparisonOperator op = toComparisonOperator(advance());
            FormulaExpression right = parseAdditive();
            left = new BinaryExpression(op, left, right);
        }
        return left;
    }

    private FormulaExpression parseAdditive() {
        FormulaExpression left = parseMultiplicative();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            ComparisonOperator op = previous().type() == TokenType.PLUS
                ? ComparisonOperator.ADD : ComparisonOperator.SUBTRACT;
            FormulaExpression right = parseMultiplicative();
            left = new BinaryExpression(op, left, right);
        }
        return left;
    }

    private FormulaExpression parseMultiplicative() {
        FormulaExpression left = parseUnary();
        while (match(TokenType.MULTIPLY, TokenType.DIVIDE)) {
            ComparisonOperator op = previous().type() == TokenType.MULTIPLY
                ? ComparisonOperator.MULTIPLY : ComparisonOperator.DIVIDE;
            FormulaExpression right = parseUnary();
            left = new BinaryExpression(op, left, right);
        }
        return left;
    }

    private FormulaExpression parseUnary() {
        if (match(TokenType.MINUS)) {
            return new BinaryExpression(
                ComparisonOperator.MULTIPLY,
                new NumberLiteralExpression(new BigDecimal("-1")),
                parseUnary()
            );
        }
        return parsePrimary();
    }

    private FormulaExpression parsePrimary() {
        if (match(TokenType.NUMBER)) {
            return new NumberLiteralExpression(new BigDecimal(previous().lexeme()));
        }
        if (match(TokenType.IDENTIFIER)) {
            String name = previous().lexeme();
            if (match(TokenType.LPAREN)) {
                List<FormulaExpression> args = parseArguments();
                consume(TokenType.RPAREN, "Esperado ')' após argumentos");
                return new FunctionCallExpression(name, args);
            }
            return parseFieldReference(name);
        }
        if (match(TokenType.LPAREN)) {
            FormulaExpression expr = parseComparison();
            consume(TokenType.RPAREN, "Esperado ')'");
            return expr;
        }
        throw new FormulaParseException("Expressão inválida", peek().position());
    }

    private FormulaExpression parseFieldReference(String firstPart) {
        StringBuilder name = new StringBuilder(firstPart);
        while (match(TokenType.DOT)) {
            Token part = consume(TokenType.IDENTIFIER, "Esperado identificador após '.'");
            name.append('.').append(part.lexeme());
        }
        return new FieldReferenceExpression(name.toString());
    }

    private List<FormulaExpression> parseArguments() {
        List<FormulaExpression> args = new ArrayList<>();
        if (peek().type() == TokenType.RPAREN) {
            return args;
        }
        do {
            args.add(parseComparison());
        } while (match(TokenType.COMMA));
        return args;
    }

    private boolean isComparisonOperator(TokenType type) {
        return type == TokenType.GT || type == TokenType.LT || type == TokenType.GTE
            || type == TokenType.LTE || type == TokenType.EQ || type == TokenType.NEQ;
    }

    private ComparisonOperator toComparisonOperator(Token token) {
        return switch (token.type()) {
            case GT -> ComparisonOperator.GT;
            case LT -> ComparisonOperator.LT;
            case GTE -> ComparisonOperator.GTE;
            case LTE -> ComparisonOperator.LTE;
            case EQ -> ComparisonOperator.EQ;
            case NEQ -> ComparisonOperator.NEQ;
            default -> throw new FormulaParseException("Operador inválido", token.position());
        };
    }

    private Token consume(TokenType type, String message) {
        if (peek().type() == type) {
            return advance();
        }
        throw new FormulaParseException(message, peek().position());
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (peek().type() == type) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token advance() {
        if (peek().type() != TokenType.EOF) {
            current++;
        }
        return previous();
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
