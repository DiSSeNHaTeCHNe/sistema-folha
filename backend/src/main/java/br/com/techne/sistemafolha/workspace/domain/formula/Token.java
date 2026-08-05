package br.com.techne.sistemafolha.workspace.domain.formula;

public record Token(
    TokenType type,
    String lexeme,
    int position
) {}
