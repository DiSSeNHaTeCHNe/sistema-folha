package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.CentroCustoDTO;
import br.com.techne.sistemafolha.exception.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.model.LinhaNegocio;
import br.com.techne.sistemafolha.repository.CentroCustoRepository;
import br.com.techne.sistemafolha.repository.LinhaNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CentroCustoService {
    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;

    public List<CentroCustoDTO> listarTodas() {
        return centroCustoRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CentroCustoDTO> listarPorLinhaNegocio(Long linhaNegocioId) {
        return centroCustoRepository.findByLinhaNegocioIdAndAtivoTrue(linhaNegocioId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CentroCustoDTO buscarPorId(Long id) {
        return centroCustoRepository.findById(id)
                .filter(cc -> cc.getAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new CentroCustoNotFoundException(id));
    }

    @Transactional
    public CentroCustoDTO cadastrar(CentroCustoDTO dto) {
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));

        CentroCusto centroCusto = toEntity(dto);
        centroCusto.setLinhaNegocio(linhaNegocio);
        return toDTO(centroCustoRepository.save(centroCusto));
    }

    @Transactional
    public CentroCustoDTO atualizar(Long id, CentroCustoDTO dto) {
        CentroCusto centroCusto = centroCustoRepository.findById(id)
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(id));

        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));

        centroCusto.setDescricao(dto.descricao());
        centroCusto.setLinhaNegocio(linhaNegocio);
        return toDTO(centroCustoRepository.save(centroCusto));
    }

    @Transactional
    public void remover(Long id) {
        CentroCusto centroCusto = centroCustoRepository.findById(id)
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(id));
        centroCusto.setAtivo(false);
        centroCustoRepository.save(centroCusto);
    }

    private CentroCustoDTO toDTO(CentroCusto centroCusto) {
        return new CentroCustoDTO(
            centroCusto.getId(),
            centroCusto.getDescricao(),
            centroCusto.getAtivo(),
            centroCusto.getLinhaNegocio().getId()
        );
    }

    private CentroCusto toEntity(CentroCustoDTO dto) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setDescricao(dto.descricao());
        centroCusto.setAtivo(true);
        return centroCusto;
    }
} 