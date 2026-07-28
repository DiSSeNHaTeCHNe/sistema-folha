package br.com.techne.sistemafolha.beneficios.infrastructure;

import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BeneficioMensalRepository extends JpaRepository<BeneficioMensal, Long> {

    List<BeneficioMensal> findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
        LocalDate competenciaInicio, LocalDate competenciaFim);

    List<BeneficioMensal> findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue(
        LocalDate competenciaInicio, LocalDate competenciaFim, Collection<Long> centroCustoIds);

    List<BeneficioMensal> findByFuncionarioIdAndCompetenciaInicioAndAtivoTrue(
        Long funcionarioId, LocalDate competenciaInicio);

    List<BeneficioMensal> findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
        Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim);

    boolean existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
        LocalDate competenciaInicio, LocalDate competenciaFim);

    @Query("""
        SELECT tb.codigo AS codigo, tb.descricao AS descricao,
               SUM(bm.valor) AS total, COUNT(bm) AS qtdLancamentos
        FROM BeneficioMensal bm
        JOIN bm.tipoBeneficio tb
        WHERE bm.ativo = true
          AND bm.competenciaInicio = :competenciaInicio
          AND bm.competenciaFim = :competenciaFim
        GROUP BY tb.id, tb.codigo, tb.descricao
        ORDER BY tb.codigo
        """)
    List<BeneficioMensalResumoProjection> resumoPorCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim);

    @Query("""
        SELECT tb.codigo AS codigo, tb.descricao AS descricao,
               SUM(bm.valor) AS total, COUNT(bm) AS qtdLancamentos
        FROM BeneficioMensal bm
        JOIN bm.tipoBeneficio tb
        WHERE bm.ativo = true
          AND bm.competenciaInicio = :competenciaInicio
          AND bm.competenciaFim = :competenciaFim
          AND bm.funcionario.centroCusto.id IN :centroCustoIds
        GROUP BY tb.id, tb.codigo, tb.descricao
        ORDER BY tb.codigo
        """)
    List<BeneficioMensalResumoProjection> resumoPorCompetenciaAndCentroCustoIds(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("centroCustoIds") Collection<Long> centroCustoIds);

    @Query("""
        SELECT bm.competenciaInicio AS competenciaInicio, bm.competenciaFim AS competenciaFim,
               COUNT(DISTINCT bm.funcionario.id) AS totalFuncionarios,
               SUM(bm.valor) AS totalBeneficios,
               COUNT(bm) AS qtdLancamentos
        FROM BeneficioMensal bm
        WHERE bm.ativo = true
          AND bm.competenciaInicio >= :dataInicio
          AND bm.competenciaFim <= :dataFim
        GROUP BY bm.competenciaInicio, bm.competenciaFim
        ORDER BY bm.competenciaInicio DESC
        """)
    List<BeneficioMensalCompetenciaProjection> competenciasResumo(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim);

    @Query("""
        SELECT bm.competenciaInicio AS competenciaInicio, bm.competenciaFim AS competenciaFim,
               COUNT(DISTINCT bm.funcionario.id) AS totalFuncionarios,
               SUM(bm.valor) AS totalBeneficios,
               COUNT(bm) AS qtdLancamentos
        FROM BeneficioMensal bm
        WHERE bm.ativo = true
          AND bm.competenciaInicio >= :dataInicio
          AND bm.competenciaFim <= :dataFim
          AND bm.funcionario.centroCusto.id IN :centroCustoIds
        GROUP BY bm.competenciaInicio, bm.competenciaFim
        ORDER BY bm.competenciaInicio DESC
        """)
    List<BeneficioMensalCompetenciaProjection> competenciasResumoAndCentroCustoIds(
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("centroCustoIds") Collection<Long> centroCustoIds);

    @Modifying
    @Query("""
        UPDATE BeneficioMensal bm
        SET bm.ativo = false
        WHERE bm.competenciaInicio = :competenciaInicio
          AND bm.competenciaFim = :competenciaFim
          AND bm.ativo = true
        """)
    void softDeleteByCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim);

    @Modifying
    @Query("""
        DELETE FROM BeneficioMensal bm
        WHERE bm.competenciaInicio = :competenciaInicio
          AND bm.competenciaFim = :competenciaFim
        """)
    void deleteByCompetenciaInicioAndCompetenciaFim(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim);

    @Query("""
        SELECT bm.funcionario.id, COALESCE(SUM(bm.valor), 0)
        FROM BeneficioMensal bm
        WHERE bm.ativo = true
          AND bm.competenciaInicio = :competenciaInicio
          AND bm.competenciaFim = :competenciaFim
          AND bm.funcionario.id IN :funcionarioIds
        GROUP BY bm.funcionario.id
        """)
    List<Object[]> sumValorPorFuncionariosECompetencia(
        @Param("funcionarioIds") Collection<Long> funcionarioIds,
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim);

    @Query("""
        SELECT COALESCE(SUM(bm.valor), 0)
        FROM BeneficioMensal bm
        WHERE bm.ativo = true
          AND bm.competenciaInicio = :competenciaInicio
          AND bm.competenciaFim = :competenciaFim
          AND bm.funcionario.centroCusto.id IN :centroCustoIds
        """)
    BigDecimal sumValorPorCompetenciaECentros(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("centroCustoIds") Collection<Long> centroCustoIds);
}
