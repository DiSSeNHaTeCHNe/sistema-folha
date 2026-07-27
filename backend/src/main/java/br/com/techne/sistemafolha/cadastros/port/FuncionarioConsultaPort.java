package br.com.techne.sistemafolha.cadastros.port;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;

import java.util.Optional;

public interface FuncionarioConsultaPort {

    Optional<Funcionario> findById(Long id);

    Optional<Funcionario> findByIdAndAtivoTrue(Long id);

    Optional<Funcionario> findByCpfAndAtivoTrue(String cpf);
}
