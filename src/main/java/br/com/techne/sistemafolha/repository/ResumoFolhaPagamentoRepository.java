package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.ResumoFolhaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResumoFolhaPagamentoRepository extends JpaRepository<ResumoFolhaPagamento, Long> {
    
    List<ResumoFolhaPagamento> findByAtivoTrue();
    
    List<ResumoFolhaPagamento> findByCompetenciaInicioBetweenAndAtivoTrue(LocalDate dataInicio, LocalDate dataFim);
    
    Optional<ResumoFolhaPagamento> findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(LocalDate competenciaInicio, LocalDate competenciaFim);
    
    @Query("SELECT r FROM ResumoFolhaPagamento r WHERE r.ativo = true ORDER BY r.dataImportacao DESC")
    List<ResumoFolhaPagamento> findLatestResumos();
} 