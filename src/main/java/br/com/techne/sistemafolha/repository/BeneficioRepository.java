package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {
    
    @Query("SELECT b FROM Beneficio b " +
           "WHERE b.funcionario.id = :funcionarioId " +
           "AND b.dataInicio <= :data " +
           "AND (b.dataFim IS NULL OR b.dataFim >= :data) " +
           "AND b.ativo = true")
    List<Beneficio> findBeneficiosAtivosByFuncionario(
        @Param("funcionarioId") Long funcionarioId,
        @Param("data") LocalDate data);
    
    @Query("SELECT b FROM Beneficio b " +
           "WHERE b.funcionario.centroCusto = :centroCusto " +
           "AND b.dataInicio <= :data " +
           "AND (b.dataFim IS NULL OR b.dataFim >= :data) " +
           "AND b.ativo = true")
    List<Beneficio> findBeneficiosAtivosByCentroCusto(
        @Param("centroCusto") CentroCusto centroCusto,
        @Param("data") LocalDate data);
    
    @Modifying
    @Query("UPDATE Beneficio b SET b.ativo = false WHERE b.id = :id")
    void softDelete(@Param("id") Long id);
    
    @Query("SELECT COUNT(b) FROM Beneficio b " +
           "WHERE b.ativo = true " +
           "AND (b.dataFim IS NULL OR b.dataFim >= :data)")
    Long countByDataFimIsNullOrDataFimAfter(@Param("data") LocalDate data);
} 