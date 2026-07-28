package br.com.techne.sistemafolha.folha.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FolhaConsultaPort {

    Optional<FolhaResumoSnapshot> findResumoMaisRecente();

    List<FolhaLinhaSnapshot> findLinhasAtivasPorCompetencia(
        LocalDate competenciaInicio, LocalDate competenciaFim, boolean decimoTerceiro, Set<Long> centrosCustoIds);

    List<FolhaEvolucaoSnapshot> findEvolucaoUltimos12Meses(LocalDate dataInicio);

    boolean existsResumoAtivo(LocalDate inicio, LocalDate fim, boolean decimoTerceiro);

    boolean existsAtivaByCpfAndCompetenciaExcludingFuncionario(
        String cpf, Long funcionarioId, LocalDate inicio, LocalDate fim, boolean decimoTerceiro);

    boolean existsByFuncionarioIdAndRubricaIdAndPeriodo(
        Long funcionarioId, Long rubricaId, LocalDate inicio, LocalDate fim, boolean decimoTerceiro);
}
