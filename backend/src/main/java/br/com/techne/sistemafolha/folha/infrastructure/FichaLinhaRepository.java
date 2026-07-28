package br.com.techne.sistemafolha.folha.infrastructure;

import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FichaLinhaRepository extends JpaRepository<FichaLinha, Long> {

    @Query("""
        SELECT fl FROM FichaLinha fl
        JOIN fl.fichaMensal fm
        WHERE fm.ativo = true
        AND fl.ativo = true
        AND fm.competenciaInicio = :competenciaInicio
        AND fm.competenciaFim = :competenciaFim
        AND fm.decimoTerceiro = :decimoTerceiro
        """)
    List<FichaLinha> findByCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);

    @Modifying
    @Query("""
        DELETE FROM FichaLinha fl
        WHERE fl.fichaMensal.id IN (
            SELECT fm.id FROM FichaMensal fm
            WHERE fm.competenciaInicio = :competenciaInicio
            AND fm.competenciaFim = :competenciaFim
            AND fm.decimoTerceiro = :decimoTerceiro
        )
        """)
    void deleteByCompetencia(
        @Param("competenciaInicio") LocalDate competenciaInicio,
        @Param("competenciaFim") LocalDate competenciaFim,
        @Param("decimoTerceiro") boolean decimoTerceiro);
}
