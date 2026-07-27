package br.com.techne.sistemafolha.cadastros.port;

import java.util.Optional;
import java.util.Set;

public interface CadastrosImportLookupPort {

    Optional<FuncionarioImportRef> findFuncionarioByIdExterno(String idExterno);

    RubricaImportRef findOrCreateRubrica(String codigo, String descricao, String tipoRubricaDescricao);

    long countFuncionariosAtivos();

    long countFuncionariosAtivosPorCentros(Set<Long> centrosCustoIds);
}
