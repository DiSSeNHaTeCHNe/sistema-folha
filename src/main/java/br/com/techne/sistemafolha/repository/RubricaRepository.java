package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Rubrica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RubricaRepository extends JpaRepository<Rubrica, Long> {
    List<Rubrica> findByAtivoTrue();
    Optional<Rubrica> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Query("UPDATE Rubrica r SET r.ativo = false WHERE r.id = :id")
    void softDelete(@Param("id") Long id);

    Optional<Rubrica> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
} 