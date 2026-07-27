package br.com.techne.sistemafolha.beneficios.infrastructure;

import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoBeneficioRepository extends JpaRepository<TipoBeneficio, Long> {
    Optional<TipoBeneficio> findByCodigoAndAtivoTrue(String codigo);
    List<TipoBeneficio> findAllByAtivoTrue();
    boolean existsByCodigo(String codigo);
}
