package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.api.TipoBeneficioDTO;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioCodigoDuplicadoException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficioNotFoundException;
import br.com.techne.sistemafolha.beneficios.domain.TipoBeneficio;
import br.com.techne.sistemafolha.beneficios.infrastructure.TipoBeneficioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TipoBeneficioService {

    private final TipoBeneficioRepository tipoBeneficioRepository;

    public List<TipoBeneficioDTO> listarAtivos() {
        return tipoBeneficioRepository.findAllByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public TipoBeneficioDTO criar(TipoBeneficioDTO dto) {
        validarCodigoUnico(dto.codigo());
        TipoBeneficio tipo = toEntity(dto);
        return toDTO(tipoBeneficioRepository.save(tipo));
    }

    @Transactional
    public TipoBeneficioDTO atualizar(Long id, TipoBeneficioDTO dto) {
        TipoBeneficio tipo = tipoBeneficioRepository.findById(id)
                .filter(t -> Boolean.TRUE.equals(t.getAtivo()))
                .orElseThrow(() -> new TipoBeneficioNotFoundException(id));

        tipo.setDescricao(dto.descricao());
        return toDTO(tipoBeneficioRepository.save(tipo));
    }

    @Transactional
    public void remover(Long id) {
        TipoBeneficio tipo = tipoBeneficioRepository.findById(id)
                .filter(t -> Boolean.TRUE.equals(t.getAtivo()))
                .orElseThrow(() -> new TipoBeneficioNotFoundException(id));
        tipo.setAtivo(false);
        tipoBeneficioRepository.save(tipo);
    }

    private void validarCodigoUnico(String codigo) {
        if (tipoBeneficioRepository.existsByCodigo(codigo)) {
            throw new TipoBeneficioCodigoDuplicadoException(codigo);
        }
    }

    private TipoBeneficioDTO toDTO(TipoBeneficio tipo) {
        return new TipoBeneficioDTO(
                tipo.getId(),
                tipo.getCodigo(),
                tipo.getDescricao(),
                tipo.getAtivo()
        );
    }

    private TipoBeneficio toEntity(TipoBeneficioDTO dto) {
        TipoBeneficio tipo = new TipoBeneficio();
        tipo.setCodigo(dto.codigo());
        tipo.setDescricao(dto.descricao());
        tipo.setAtivo(true);
        return tipo;
    }
}
