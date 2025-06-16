package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.BeneficioDTO;
import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.repository.BeneficioRepository;
import br.com.techne.sistemafolha.repository.CentroCustoRepository;
import br.com.techne.sistemafolha.exception.BeneficioNotFoundException;
import br.com.techne.sistemafolha.exception.CentroCustoNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficioService {
    private final BeneficioRepository beneficioRepository;
    private final CentroCustoRepository centroCustoRepository;

    public List<BeneficioDTO> consultarPorFuncionario(Long funcionarioId, LocalDate data) {
        return beneficioRepository.findBeneficiosAtivosByFuncionario(funcionarioId, data)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<BeneficioDTO> consultarPorCentroCusto(Long centroCustoId, LocalDate data) {
        CentroCusto centroCusto = centroCustoRepository.findById(centroCustoId)
            .orElseThrow(() -> new CentroCustoNotFoundException(centroCustoId));
            
        return beneficioRepository.findBeneficiosAtivosByCentroCusto(centroCusto, data)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public void remover(Long id) {
        Beneficio beneficio = beneficioRepository.findById(id)
            .orElseThrow(() -> new BeneficioNotFoundException("Benefício não encontrado com ID: " + id));
        
        beneficioRepository.softDelete(id);
    }

    private BeneficioDTO toDTO(Beneficio beneficio) {
        return new BeneficioDTO(
            beneficio.getId(),
            beneficio.getFuncionario().getId(),
            beneficio.getFuncionario().getNome(),
            beneficio.getDescricao(),
            beneficio.getValor(),
            beneficio.getDataInicio(),
            beneficio.getDataFim(),
            beneficio.getObservacao()
        );
    }
} 