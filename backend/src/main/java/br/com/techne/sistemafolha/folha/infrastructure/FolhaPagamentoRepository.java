package br.com.techne.sistemafolha.folha.infrastructure;

import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import org.springframework.data.jpa.repository.EntityGraph;
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
    List<FolhaPagamento> findByCentroCustoAndPeriodo(CentroCusto centroCusto, LocalDate dataInicio, LocalDate dataFim);
    
    boolean existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim(
        Long funcionarioId, Long rubricaId, LocalDate dataInicio, LocalDate dataFim);

    List<FolhaPagamento> findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(Long funcionarioId, LocalDate dataInicio, LocalDate dataFim);

    @Query("""
        SELECT f FROM FolhaPagamento f
        WHERE f.ativo = true
        AND f.funcionario.id = :funcionarioId
        AND f.dataInicio = :dataInicio
        AND f.dataFim = :dataFim
        AND f.decimoTerceiro = :decimoTerceiro
        """)
    List<FolhaPagamento> findByFuncionarioIdAndCompetenciaAndDecimoTerceiroAndAtivoTrue(
        @Param("funcionarioId") Long funcionarioId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);
    List<FolhaPagamento> findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(CentroCusto centroCusto, LocalDate dataInicio, LocalDate dataFim);
    List<FolhaPagamento> findByCentroCustoAndDataInicioBetweenAndAtivoTrue(CentroCusto centroCusto, LocalDate dataInicio, LocalDate dataFim);
    List<FolhaPagamento> findByDataInicioBetweenAndAtivoTrue(LocalDate dataInicio, LocalDate dataFim);
    
    @Query("SELECT f FROM FolhaPagamento f WHERE f.linhaNegocio = :linhaNegocio AND f.dataInicio BETWEEN :dataInicio AND :dataFim AND f.ativo = true")
    List<FolhaPagamento> findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue(
        @Param("linhaNegocio") LinhaNegocio linhaNegocio, 
        @Param("dataInicio") LocalDate dataInicio, 
        @Param("dataFim") LocalDate dataFim
    );
    
    @Modifying
    @Query("UPDATE FolhaPagamento f SET f.ativo = false WHERE f.id = :id")
    void softDelete(@Param("id") Long id);
    
    List<FolhaPagamento> findByFuncionarioIdAndAtivoTrue(Long funcionarioId);
    
    // Métodos para buscar dados da competência mais recente
    @Query("SELECT f FROM FolhaPagamento f WHERE f.ativo = true AND f.dataInicio = :competenciaInicio AND f.dataFim = :competenciaFim AND f.decimoTerceiro = :decimoTerceiro")
    List<FolhaPagamento> findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);

    @EntityGraph(attributePaths = {
        "funcionario",
        "funcionario.centroCusto",
        "funcionario.centroCusto.linhaNegocio",
        "funcionario.cargo",
        "cargo",
        "centroCusto",
        "linhaNegocio",
        "rubrica",
        "rubrica.tipoRubrica"
    })
    @Query("""
        SELECT f FROM FolhaPagamento f
        WHERE f.ativo = true
        AND f.dataInicio = :competenciaInicio
        AND f.dataFim = :competenciaFim
        AND f.decimoTerceiro = :decimoTerceiro
        """)
    List<FolhaPagamento> findByCompetenciaAndDecimoTerceiroWithFetch(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);
    
    List<FolhaPagamento> findByDataInicioAndDataFimAndDecimoTerceiro(
        LocalDate dataInicio, LocalDate dataFim, boolean decimoTerceiro);

    @Query("""
        SELECT COUNT(f) > 0 FROM FolhaPagamento f
        WHERE f.ativo = true
        AND f.dataInicio = :dataInicio
        AND f.dataFim = :dataFim
        AND f.decimoTerceiro = :decimoTerceiro
        AND f.funcionario.cpf = :cpf
        AND f.funcionario.id <> :funcionarioId
        """)
    boolean existsAtivaByCpfAndCompetenciaExcludingFuncionario(
        @Param("cpf") String cpf,
        @Param("funcionarioId") Long funcionarioId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);

    @Query("""
        SELECT COUNT(f) > 0 FROM FolhaPagamento f
        WHERE f.ativo = true
        AND f.funcionario.id = :funcionarioId
        AND f.rubrica.id = :rubricaId
        AND f.dataInicio = :dataInicio
        AND f.dataFim = :dataFim
        AND f.decimoTerceiro = :decimoTerceiro
        """)
    boolean existsByFuncionarioIdAndRubricaIdAndPeriodoAndDecimoTerceiro(
        @Param("funcionarioId") Long funcionarioId,
        @Param("rubricaId") Long rubricaId,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);
} 