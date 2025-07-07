package br.com.techne.sistemafolha.dto;

import br.com.techne.sistemafolha.model.Usuario;
import java.util.List;

public record UsuarioDTO(
    Long id,
    String login,
    String senha,
    String nome,
    List<String> permissoes
) {
    public static UsuarioDTO fromEntity(Usuario usuario) {
        return new UsuarioDTO(
            usuario.getId(),
            usuario.getLogin(),
            null, // Não retornamos a senha no DTO
            usuario.getNome(),
            usuario.getPermissoes()
        );
    }
} 