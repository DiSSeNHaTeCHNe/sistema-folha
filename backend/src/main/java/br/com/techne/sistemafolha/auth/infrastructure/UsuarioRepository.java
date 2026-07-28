package br.com.techne.sistemafolha.auth.infrastructure;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    List<Usuario> findByAtivoTrue();
    Optional<Usuario> findByLoginAndAtivoTrue(String login);
    boolean existsByLoginAndAtivoTrue(String login);
    Optional<Usuario> findByFuncionarioIdAndAtivoTrue(Long funcionarioId);

    @Query("""
        SELECT u FROM Usuario u
        LEFT JOIN u.funcionario f
        WHERE u.ativo = true
          AND (:nomePattern IS NULL OR u.nome ILIKE :nomePattern)
          AND (:loginPattern IS NULL OR u.login ILIKE :loginPattern)
          AND (:funcionarioId IS NULL OR f.id = :funcionarioId)
        ORDER BY u.nome ASC, u.login ASC
        """)
    List<Usuario> findByFiltros(
        @Param("nomePattern") String nomePattern,
        @Param("loginPattern") String loginPattern,
        @Param("funcionarioId") Long funcionarioId
    );
} 