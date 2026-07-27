package br.com.techne.sistemafolha.auth.port;

import br.com.techne.sistemafolha.auth.domain.Usuario;

import java.util.Optional;

public interface UsuarioLookupPort {

    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByLoginAndAtivoTrue(String login);
}
