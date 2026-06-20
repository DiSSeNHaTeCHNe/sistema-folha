package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.TipoRubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoRubricaRepository extends JpaRepository<TipoRubrica, Long> {
    
    Optional<TipoRubrica> findByDescricao(String descricao);
    
    Optional<TipoRubrica> findByDescricaoAndAtivoTrue(String descricao);
} 