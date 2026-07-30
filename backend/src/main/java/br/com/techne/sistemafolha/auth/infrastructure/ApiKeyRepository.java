package br.com.techne.sistemafolha.auth.infrastructure;

import br.com.techne.sistemafolha.auth.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByPrefixoAndRevogadoFalse(String prefixo);

    List<ApiKey> findByUsuarioIdOrderByDataCriacaoDesc(Long usuarioId);
}
