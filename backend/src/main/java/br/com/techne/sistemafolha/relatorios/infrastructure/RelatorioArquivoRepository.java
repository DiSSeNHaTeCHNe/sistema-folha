package br.com.techne.sistemafolha.relatorios.infrastructure;

import br.com.techne.sistemafolha.relatorios.domain.RelatorioArquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RelatorioArquivoRepository extends JpaRepository<RelatorioArquivo, Long> {

    Optional<RelatorioArquivo> findByRelatorioId(Long relatorioId);
}
