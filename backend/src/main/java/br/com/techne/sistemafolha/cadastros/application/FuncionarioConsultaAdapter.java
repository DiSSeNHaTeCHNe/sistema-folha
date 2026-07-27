package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioConsultaPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FuncionarioConsultaAdapter implements FuncionarioConsultaPort {

    private final FuncionarioRepository funcionarioRepository;

    @Override
    public Optional<Funcionario> findById(Long id) {
        return funcionarioRepository.findById(id);
    }

    @Override
    public Optional<Funcionario> findByIdAndAtivoTrue(Long id) {
        return funcionarioRepository.findByIdAndAtivoTrue(id);
    }

    @Override
    public Optional<Funcionario> findByCpfAndAtivoTrue(String cpf) {
        return funcionarioRepository.findByCpfAndAtivoTrue(cpf);
    }
}
