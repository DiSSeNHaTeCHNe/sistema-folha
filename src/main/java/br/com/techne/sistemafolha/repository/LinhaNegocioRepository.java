package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.LinhaNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinhaNegocioRepository extends JpaRepository<LinhaNegocio, Long> {
    List<LinhaNegocio> findByAtivoTrue();
    Optional<LinhaNegocio> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Transactional
    @Query("UPDATE LinhaNegocio l SET l.ativo = false WHERE l.id = :id")
    void softDelete(@Param("id") Long id);
} 