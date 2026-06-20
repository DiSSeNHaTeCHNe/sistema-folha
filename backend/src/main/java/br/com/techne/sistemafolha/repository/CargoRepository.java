package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CargoRepository extends JpaRepository<Cargo, Long> {
} 