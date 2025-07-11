package br.com.techne.sistemafolha.dto;

import br.com.techne.sistemafolha.model.Usuario;
import java.util.List;

public record UsuarioDTO(
    Long id,
    String login,
    String senha,
    String nome,
    List<String> permissoes,
    Long funcionarioId,
    String funcionarioNome,
    String funcionarioCpf
) {
    public static UsuarioDTO fromEntity(Usuario usuario) {
        return new UsuarioDTO(
            usuario.getId(),
            usuario.getLogin(),
            null, // Não retornamos a senha no DTO
            usuario.getNome(),
            usuario.getPermissoes(),
            usuario.getFuncionario() != null ? usuario.getFuncionario().getId() : null,
            usuario.getFuncionario() != null ? usuario.getFuncionario().getNome() : null,
            usuario.getFuncionario() != null ? usuario.getFuncionario().getCpf() : null
        );
    }
} 