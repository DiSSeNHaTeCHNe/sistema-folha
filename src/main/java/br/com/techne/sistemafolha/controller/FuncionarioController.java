package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.FuncionarioDTO;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionários", description = "API para gerenciamento de funcionários")
public class FuncionarioController {
    private final FuncionarioRepository funcionarioRepository;

    @GetMapping
    @Operation(summary = "Lista todos os funcionários ativos")
    public ResponseEntity<List<FuncionarioDTO>> listar() {
        List<FuncionarioDTO> funcionarios = funcionarioRepository.findAll().stream()
            .filter(Funcionario::getAtivo)
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(funcionarios);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um funcionário ativo pelo ID")
    public ResponseEntity<FuncionarioDTO> buscarPorId(@PathVariable Long id) {
        return funcionarioRepository.findById(id)
            .filter(Funcionario::getAtivo)
            .map(funcionario -> ResponseEntity.ok(toDTO(funcionario)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo funcionário")
    public ResponseEntity<FuncionarioDTO> cadastrar(@Valid @RequestBody FuncionarioDTO dto) {
        if (funcionarioRepository.existsByIdExterno(dto.idExterno())) {
            return ResponseEntity.badRequest().build();
        }

        Funcionario funcionario = toEntity(dto);
        funcionario = funcionarioRepository.save(funcionario);
        return ResponseEntity.ok(toDTO(funcionario));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um funcionário existente")
    public ResponseEntity<FuncionarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioDTO dto) {
        return funcionarioRepository.findById(id)
            .filter(Funcionario::getAtivo)
            .map(funcionario -> {
                Funcionario funcionarioAtualizado = toEntity(dto);
                funcionarioAtualizado.setId(id);
                funcionarioAtualizado = funcionarioRepository.save(funcionarioAtualizado);
                return ResponseEntity.ok(toDTO(funcionarioAtualizado));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um funcionário (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return funcionarioRepository.findById(id)
            .filter(Funcionario::getAtivo)
            .map(funcionario -> {
                funcionarioRepository.softDelete(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private FuncionarioDTO toDTO(Funcionario funcionario) {
        return new FuncionarioDTO(
            funcionario.getId(),
            funcionario.getNome(),
            funcionario.getCargo(),
            funcionario.getCentroCusto(),
            funcionario.getLinhaNegocio(),
            funcionario.getIdExterno(),
            funcionario.getDataAdmissao(),
            funcionario.getSexo(),
            funcionario.getTipoSalario(),
            funcionario.getFuncao(),
            funcionario.getDepIrrf(),
            funcionario.getDepSalFamilia(),
            funcionario.getVinculo()
        );
    }

    private Funcionario toEntity(FuncionarioDTO dto) {
        Funcionario funcionario = new Funcionario();
        funcionario.setNome(dto.nome());
        funcionario.setCargo(dto.cargo());
        funcionario.setCentroCusto(dto.centroCusto());
        funcionario.setLinhaNegocio(dto.linhaNegocio());
        funcionario.setIdExterno(dto.idExterno());
        funcionario.setDataAdmissao(dto.dataAdmissao());
        funcionario.setSexo(dto.sexo());
        funcionario.setTipoSalario(dto.tipoSalario());
        funcionario.setFuncao(dto.funcao());
        funcionario.setDepIrrf(dto.depIrrf());
        funcionario.setDepSalFamilia(dto.depSalFamilia());
        funcionario.setVinculo(dto.vinculo());
        funcionario.setAtivo(true);
        return funcionario;
    }
} 