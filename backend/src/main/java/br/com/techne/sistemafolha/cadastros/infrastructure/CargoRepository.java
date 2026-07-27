package br.com.techne.sistemafolha.cadastros.infrastructure;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {
} 