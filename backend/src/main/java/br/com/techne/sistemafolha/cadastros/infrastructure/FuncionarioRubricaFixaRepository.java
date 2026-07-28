package br.com.techne.sistemafolha.cadastros.infrastructure;

import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FuncionarioRubricaFixaRepository extends JpaRepository<FuncionarioRubricaFixa, Long> {

    @Query("""
        SELECT f FROM FuncionarioRubricaFixa f
        JOIN FETCH f.funcionario
        JOIN FETCH f.rubrica r
        JOIN FETCH r.tipoRubrica
        WHERE f.ativo = true
        AND (:funcionarioId IS NULL OR f.funcionario.id = :funcionarioId)
        AND (:rubricaId IS NULL OR f.rubrica.id = :rubricaId)
        ORDER BY f.vigenciaInicio DESC, f.id DESC
        """)
    List<FuncionarioRubricaFixa> findByFiltros(
        @Param("funcionarioId") Long funcionarioId,
        @Param("rubricaId") Long rubricaId);

    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM FuncionarioRubricaFixa f
        WHERE f.ativo = true
        AND f.funcionario.id = :funcionarioId
        AND f.rubrica.id = :rubricaId
        AND (:excludeId IS NULL OR f.id <> :excludeId)
        AND f.vigenciaInicio <= COALESCE(:vigenciaFim, :vigenciaInicio)
        AND COALESCE(f.vigenciaFim, :vigenciaInicio) >= :vigenciaInicio
        """)
    boolean existsVigenciaSobreposta(
        @Param("funcionarioId") Long funcionarioId,
        @Param("rubricaId") Long rubricaId,
        @Param("vigenciaInicio") LocalDate vigenciaInicio,
        @Param("vigenciaFim") LocalDate vigenciaFim,
        @Param("excludeId") Long excludeId);

    @Query("""
        SELECT f FROM FuncionarioRubricaFixa f
        JOIN FETCH f.funcionario
        JOIN FETCH f.rubrica r
        JOIN FETCH r.tipoRubrica
        WHERE f.ativo = true
        AND f.vigenciaInicio <= :competenciaFim
        AND COALESCE(f.vigenciaFim, :competenciaInicio) >= :competenciaInicio
        """)
    List<FuncionarioRubricaFixa> findVigentesNaCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim);
}
