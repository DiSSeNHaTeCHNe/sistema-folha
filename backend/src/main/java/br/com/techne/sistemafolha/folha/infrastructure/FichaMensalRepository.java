package br.com.techne.sistemafolha.folha.infrastructure;

import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FichaMensalRepository extends JpaRepository<FichaMensal, Long> {

    @Query("""
        SELECT f FROM FichaMensal f
        WHERE f.ativo = true
        AND f.competenciaInicio = :competenciaInicio
        AND f.competenciaFim = :competenciaFim
        AND f.decimoTerceiro = :decimoTerceiro
        """)
    List<FichaMensal> findByCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);

    @Query("""
        SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END
        FROM FichaMensal f
        WHERE f.ativo = true
        AND f.competenciaInicio = :competenciaInicio
        AND f.competenciaFim = :competenciaFim
        AND f.decimoTerceiro = :decimoTerceiro
        """)
    boolean existsByCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);

    @Modifying
    @Query("""
        DELETE FROM FichaMensal f
        WHERE f.competenciaInicio = :competenciaInicio
        AND f.competenciaFim = :competenciaFim
        AND f.decimoTerceiro = :decimoTerceiro
        """)
    void deleteByCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);
}
