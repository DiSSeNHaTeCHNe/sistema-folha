package br.com.techne.sistemafolha.auth.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.infrastructure.UsuarioRepository;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioLookupAdapter implements UsuarioLookupPort {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Optional<Usuario> findById(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> findByLoginAndAtivoTrue(String login) {
        return usuarioRepository.findByLoginAndAtivoTrue(login);
    }
}
