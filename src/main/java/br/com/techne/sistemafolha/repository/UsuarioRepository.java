package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByAtivoTrue();
    Optional<Usuario> findByLoginAndAtivoTrue(String login);
    boolean existsByLoginAndAtivoTrue(String login);
} 