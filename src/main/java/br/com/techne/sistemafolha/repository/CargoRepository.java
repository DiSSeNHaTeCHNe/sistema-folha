package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {
    List<Cargo> findByAtivoTrue();
    Optional<Cargo> findByCodigoAndAtivoTrue(String codigo);
    boolean existsByCodigoAndAtivoTrue(String codigo);
    List<Cargo> findByLinhaNegocioIdAndAtivoTrue(Long linhaNegocioId);
} 