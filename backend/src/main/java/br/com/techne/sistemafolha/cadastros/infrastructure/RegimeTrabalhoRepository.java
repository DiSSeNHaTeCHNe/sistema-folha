package br.com.techne.sistemafolha.cadastros.infrastructure;

import br.com.techne.sistemafolha.cadastros.domain.RegimeTrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegimeTrabalhoRepository extends JpaRepository<RegimeTrabalho, Long> {

    Optional<RegimeTrabalho> findByCodigoAndAtivoTrue(String codigo);
}
