package br.com.techne.sistemafolha.dto;

import java.util.Set;

public record UsuarioDTO(
    Long id,
    String login,
    String nome,
    String centroCusto,
    Set<String> permissoes,
    boolean primeiroAcesso
) {} 