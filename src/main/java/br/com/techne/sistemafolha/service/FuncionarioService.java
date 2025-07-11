package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.FuncionarioDTO;
import br.com.techne.sistemafolha.exception.CargoNotFoundException;
import br.com.techne.sistemafolha.exception.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.exception.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.model.Cargo;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.repository.CargoRepository;
import br.com.techne.sistemafolha.repository.CentroCustoRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;
    private final CargoRepository cargoRepository;
    private final CentroCustoRepository centroCustoRepository;

    public List<FuncionarioDTO> listarTodos() {
        return funcionarioRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<FuncionarioDTO> listarComFiltros(String nome, Long cargoId, Long centroCustoId, Long linhaNegocioId) {
        // Preparar pattern para busca de nome (case insensitive)
        String nomePattern = null;
        if (nome != null && !nome.trim().isEmpty()) {
            nomePattern = "%" + nome + "%";
        }
        
        return funcionarioRepository.findByFiltros(nomePattern, cargoId, centroCustoId, linhaNegocioId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public FuncionarioDTO buscarPorId(Long id) {
        return funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .map(this::toDTO)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
    }

    @Transactional
    public FuncionarioDTO cadastrar(FuncionarioDTO dto) {
        if (funcionarioRepository.existsByCpfAndAtivoTrue(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um funcionário ativo com este CPF");
        }

        Cargo cargo = cargoRepository.findById(dto.cargoId())
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(dto.cargoId()));

        CentroCusto centroCusto = centroCustoRepository.findById(dto.centroCustoId())
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(dto.centroCustoId()));

        Funcionario funcionario = toEntity(dto);
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        return toDTO(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public FuncionarioDTO atualizar(Long id, FuncionarioDTO dto) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));

        if (!funcionario.getCpf().equals(dto.cpf()) && 
            funcionarioRepository.existsByCpfAndAtivoTrue(dto.cpf())) {
            throw new IllegalArgumentException("Já existe um funcionário ativo com este CPF");
        }

        Cargo cargo = cargoRepository.findById(dto.cargoId())
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(dto.cargoId()));

        CentroCusto centroCusto = centroCustoRepository.findById(dto.centroCustoId())
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(dto.centroCustoId()));

        funcionario.setNome(dto.nome());
        funcionario.setCpf(dto.cpf());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setIdExterno(dto.idExterno());
        funcionario.setCargo(cargo);
        funcionario.setCentroCusto(centroCusto);
        return toDTO(funcionarioRepository.save(funcionario));
    }

    @Transactional
    public void remover(Long id) {
        Funcionario funcionario = funcionarioRepository.findById(id)
                .filter(Funcionario::getAtivo)
                .orElseThrow(() -> new FuncionarioNotFoundException(id));
        funcionario.setAtivo(false);
        funcionarioRepository.save(funcionario);
    }

    private FuncionarioDTO toDTO(Funcionario funcionario) {
        return new FuncionarioDTO(
            funcionario.getId(),
            funcionario.getNome(),
            funcionario.getCpf(),
            funcionario.getDataAdmissao(),
            funcionario.getCargo().getId(),
            funcionario.getCargo().getDescricao(),
            funcionario.getCentroCusto().getId(),
            funcionario.getCentroCusto().getDescricao(),
            funcionario.getCentroCusto().getLinhaNegocio().getId(),
            funcionario.getCentroCusto().getLinhaNegocio().getDescricao(),
            funcionario.getIdExterno(),
            funcionario.getAtivo()
        );
    }

    private Funcionario toEntity(FuncionarioDTO dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.nome());
        funcionario.setCpf(dto.cpf());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setIdExterno(dto.idExterno());
        funcionario.setAtivo(true);
        return funcionario;
    }
} 