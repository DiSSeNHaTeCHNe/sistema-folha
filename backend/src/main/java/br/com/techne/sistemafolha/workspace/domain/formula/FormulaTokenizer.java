package br.com.techne.sistemafolha.workspace.domain.formula;

import java.util.ArrayList;
import java.util.List;

public class FormulaTokenizer {

    public List<Token> tokenize(String input) {
        if (input == null || input.isBlank()) {
            throw new FormulaParseException("Expressão vazia");
        }
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && i + 1 < input.length() && Character.isDigit(input.charAt(i + 1)))) {
                int start = i;
                i++;
                while (i < input.length()) {
                    char next = input.charAt(i);
                    if (Character.isDigit(next) || next == '.') {
                        i++;
                    } else {
                        break;
                    }
                }
                tokens.add(new Token(TokenType.NUMBER, input.substring(start, i), start));
                continue;
            }
            if (isIdentifierStart(c)) {
                int start = i;
                i++;
                while (i < input.length() && isIdentifierPart(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.IDENTIFIER, input.substring(start, i), start));
                continue;
            }
            switch (c) {
                case '+' -> tokens.add(new Token(TokenType.PLUS, "+", i));
                case '-' -> tokens.add(new Token(TokenType.MINUS, "-", i));
                case '*' -> tokens.add(new Token(TokenType.MULTIPLY, "*", i));
                case '/' -> tokens.add(new Token(TokenType.DIVIDE, "/", i));
                case '(' -> tokens.add(new Token(TokenType.LPAREN, "(", i));
                case ')' -> tokens.add(new Token(TokenType.RPAREN, ")", i));
                case ',' -> tokens.add(new Token(TokenType.COMMA, ",", i));
                case '.' -> tokens.add(new Token(TokenType.DOT, ".", i));
                case '>' -> {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '=') {
                        tokens.add(new Token(TokenType.GTE, ">=", i));
                        i++;
                    } else {
                        tokens.add(new Token(TokenType.GT, ">", i));
                    }
                }
                case '<' -> {
                    if (i + 1 < input.length() && input.charAt(i + 1) == '=') {
                        tokens.add(new Token(TokenType.LTE, "<=", i));
                        i++;
                    } else if (i + 1 < input.length() && input.charAt(i + 1) == '>') {
                        tokens.add(new Token(TokenType.NEQ, "<>", i));
                        i++;
                    } else {
                        tokens.add(new Token(TokenType.LT, "<", i));
                    }
                }
                case '=' -> tokens.add(new Token(TokenType.EQ, "=", i));
                default -> throw new FormulaParseException("Caractere inválido: '" + c + "'", i);
            }
            i++;
        }
        tokens.add(new Token(TokenType.EOF, "", input.length()));
        return tokens;
    }

    private boolean isIdentifierStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
