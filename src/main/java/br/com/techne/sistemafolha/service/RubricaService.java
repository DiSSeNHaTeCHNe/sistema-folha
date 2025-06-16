package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.RubricaDTO;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.repository.RubricaRepository;
import br.com.techne.sistemafolha.exception.RubricaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricaService {
    private final RubricaRepository rubricaRepository;

    public List<RubricaDTO> listarTodas() {
        return rubricaRepository.findByAtivoTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public RubricaDTO buscarPorId(Long id) {
        return rubricaRepository.findByIdAndAtivoTrue(id)
            .map(this::toDTO)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));
    }

    @Transactional
    public RubricaDTO cadastrar(RubricaDTO dto) {
        if (rubricaRepository.existsByCodigo(dto.codigo())) {
            throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.codigo());
        }

        Rubrica rubrica = toEntity(dto);
        rubrica = rubricaRepository.save(rubrica);
        return toDTO(rubrica);
    }

    @Transactional
    public RubricaDTO atualizar(Long id, RubricaDTO dto) {
        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(id)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));

        if (!rubrica.getCodigo().equals(dto.codigo()) && rubricaRepository.existsByCodigo(dto.codigo())) {
            throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.codigo());
        }

        Rubrica rubricaAtualizada = toEntity(dto);
        rubricaAtualizada.setId(id);
        rubricaAtualizada = rubricaRepository.save(rubricaAtualizada);
        return toDTO(rubricaAtualizada);
    }

    @Transactional
    public void remover(Long id) {
        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(id)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));

        rubricaRepository.softDelete(id);
    }

    private RubricaDTO toDTO(Rubrica rubrica) {
        return new RubricaDTO(
            rubrica.getId(),
            rubrica.getCodigo(),
            rubrica.getDescricao(),
            rubrica.getTipo(),
            rubrica.getPorcentagem()
        );
    }

    private Rubrica toEntity(RubricaDTO dto) {
        Rubrica rubrica = new Rubrica();
        rubrica.setCodigo(dto.codigo());
        rubrica.setDescricao(dto.descricao());
        rubrica.setTipo(dto.tipo());
        rubrica.setPorcentagem(dto.porcentagem());
        rubrica.setAtivo(true);
        return rubrica;
    }
} 