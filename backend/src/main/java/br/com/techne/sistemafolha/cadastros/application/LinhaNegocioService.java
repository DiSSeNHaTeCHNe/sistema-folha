package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.LinhaNegocioDTO;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.infrastructure.LinhaNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinhaNegocioService {
    private final LinhaNegocioRepository linhaNegocioRepository;

    public List<LinhaNegocioDTO> listarTodas() {
        return linhaNegocioRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public LinhaNegocioDTO buscarPorId(Long id) {
        return linhaNegocioRepository.findById(id)
                .filter(ln -> ln.getAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new LinhaNegocioNotFoundException(id));
    }

    @Transactional
    public LinhaNegocioDTO cadastrar(LinhaNegocioDTO dto) {
        LinhaNegocio linhaNegocio = toEntity(dto);
        return toDTO(linhaNegocioRepository.save(linhaNegocio));
    }

    @Transactional
    public LinhaNegocioDTO atualizar(Long id, LinhaNegocioDTO dto) {
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(id)
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(id));

        linhaNegocio.setDescricao(dto.descricao());
        return toDTO(linhaNegocioRepository.save(linhaNegocio));
    }

    @Transactional
    public void remover(Long id) {
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(id)
                .filter(ln -> ln.getAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(id));
        linhaNegocio.setAtivo(false);
        linhaNegocioRepository.save(linhaNegocio);
    }

    private LinhaNegocioDTO toDTO(LinhaNegocio linhaNegocio) {
        return new LinhaNegocioDTO(
            linhaNegocio.getId(),
            linhaNegocio.getDescricao(),
            linhaNegocio.getAtivo()
        );
    }

    private LinhaNegocio toEntity(LinhaNegocioDTO dto) {
        LinhaNegocio linhaNegocio = new LinhaNegocio();
        linhaNegocio.setDescricao(dto.descricao());
        linhaNegocio.setAtivo(true);
        return linhaNegocio;
    }
} 