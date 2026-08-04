package br.com.techne.sistemafolha.relatorios.infrastructure;

import br.com.techne.sistemafolha.relatorios.domain.Relatorio;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelatorioRepository extends JpaRepository<Relatorio, Long> {

    List<Relatorio> findByTipoAndAtivoTrueOrderByAnoDescMesDesc(RelatorioTipo tipo);

    List<Relatorio> findByUsuarioIdAndTipoAndAtivoTrueOrderByAnoDescMesDesc(Long usuarioId, RelatorioTipo tipo);

    List<Relatorio> findByUsuarioIdAndStatusAndAtivoTrue(
        Long usuarioId, br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus status);

    Optional<Relatorio> findByIdAndUsuarioIdAndAtivoTrue(Long id, Long usuarioId);

    Optional<Relatorio> findByUsuarioIdAndTipoAndMesAndAnoAndAtivoTrue(
        Long usuarioId, RelatorioTipo tipo, Integer mes, Integer ano);

    long countByUsuarioIdAndStatusAndAtivoTrue(
        Long usuarioId, br.com.techne.sistemafolha.relatorios.domain.RelatorioStatus status);
}
