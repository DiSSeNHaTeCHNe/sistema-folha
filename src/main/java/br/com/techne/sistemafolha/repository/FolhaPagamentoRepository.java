package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.FolhaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FolhaPagamentoRepository extends JpaRepository<FolhaPagamento, Long> {
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.funcionario.id = :funcionarioId AND f.dataInicio = :dataInicio AND f.dataFim = :dataFim")
    List<FolhaPagamento> findByFuncionarioAndPeriodo(Long funcionarioId, LocalDate dataInicio, LocalDate dataFim);
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.funcionario.centroCusto = :centroCusto AND f.dataInicio = :dataInicio AND f.dataFim = :dataFim")
    List<FolhaPagamento> findByCentroCustoAndPeriodo(String centroCusto, LocalDate dataInicio, LocalDate dataFim);
    
    boolean existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim(
        Long funcionarioId, Long rubricaId, LocalDate dataInicio, LocalDate dataFim);

    List<FolhaPagamento> findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(Long funcionarioId, LocalDate dataInicio, LocalDate dataFim);
    List<FolhaPagamento> findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(String centroCusto, LocalDate dataInicio, LocalDate dataFim);
    
    @Modifying
    @Query("UPDATE FolhaPagamento f SET f.ativo = false WHERE f.id = :id")
    void softDelete(@Param("id") Long id);
} 