package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.RubricaDTO;
import br.com.techne.sistemafolha.cadastros.api.RubricaStatusFiltro;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.TipoRubricaRepository;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RubricaService {

    private static final Set<Short> OPERADORES_VALIDOS = Set.of((short) -1, (short) 0, (short) 1);

    private final RubricaRepository rubricaRepository;
    private final TipoRubricaRepository tipoRubricaRepository;

    public List<RubricaDTO> listarTodas() {
        return rubricaRepository.findByAtivoTrue().stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<RubricaDTO> listar(String codigo, String descricao, RubricaStatusFiltro status) {
        String codigoPattern = null;
        if (codigo != null && !codigo.trim().isEmpty()) {
            codigoPattern = "%" + codigo.trim() + "%";
        }

        String descricaoPattern = null;
        if (descricao != null && !descricao.trim().isEmpty()) {
            descricaoPattern = "%" + descricao.trim() + "%";
        }

        return rubricaRepository
                .findByFiltros(codigoPattern, descricaoPattern, resolverAtivo(status))
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private Boolean resolverAtivo(RubricaStatusFiltro status) {
        if (status == null || status == RubricaStatusFiltro.ATIVO) {
            return true;
        }
        if (status == RubricaStatusFiltro.INATIVO) {
            return false;
        }
        return null;
    }

    public RubricaDTO buscarPorId(Long id) {
        return rubricaRepository.findByIdAndAtivoTrue(id)
            .map(this::toDTO)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));
    }

    @Transactional
    public RubricaDTO cadastrar(RubricaDTO dto) {
        if (rubricaRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.getCodigo());
        }

        Rubrica rubrica = toEntity(dto);
        rubrica = rubricaRepository.save(rubrica);
        return toDTO(rubrica);
    }

    @Transactional
    public RubricaDTO atualizar(Long id, RubricaDTO dto) {
        Rubrica rubrica = rubricaRepository.findByIdAndAtivoTrue(id)
            .orElseThrow(() -> new RubricaNotFoundException("Rubrica não encontrada com ID: " + id));

        if (!rubrica.getCodigo().equals(dto.getCodigo()) && rubricaRepository.existsByCodigo(dto.getCodigo())) {
            throw new IllegalArgumentException("Já existe uma rubrica com o código: " + dto.getCodigo());
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
        String tipoDescricao = rubrica.getTipoRubrica() != null ? rubrica.getTipoRubrica().getDescricao() : null;
        return new RubricaDTO(
            rubrica.getId(),
            rubrica.getCodigo(),
            rubrica.getDescricao(),
            tipoDescricao,
            tipoDescricao,
            rubrica.getPorcentagem(),
            rubrica.getOperadorBruto(),
            rubrica.getOperadorLiquido(),
            rubrica.getOperadorCusto(),
            rubrica.getAtivo()
        );
    }

    private Rubrica toEntity(RubricaDTO dto) {
        Rubrica rubrica = new Rubrica();
        rubrica.setCodigo(dto.getCodigo());
        rubrica.setDescricao(dto.getDescricao());
        if (dto.getTipoRubricaDescricao() != null) {
            TipoRubrica tipo = tipoRubricaRepository.findByDescricao(dto.getTipoRubricaDescricao())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de rubrica não encontrado: " + dto.getTipoRubricaDescricao()));
            rubrica.setTipoRubrica(tipo);
        }
        rubrica.setPorcentagem(dto.getPorcentagem());
        rubrica.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        configurarOperadores(rubrica, dto);
        return rubrica;
    }

    private void configurarOperadores(Rubrica rubrica, RubricaDTO dto) {
        if (dto.getOperadorBruto() != null || dto.getOperadorLiquido() != null || dto.getOperadorCusto() != null) {
            if (dto.getOperadorBruto() == null || dto.getOperadorLiquido() == null || dto.getOperadorCusto() == null) {
                throw new IllegalArgumentException("Todos os operadores (bruto, líquido e custo) devem ser informados");
            }
            validarOperador(dto.getOperadorBruto(), "operadorBruto");
            validarOperador(dto.getOperadorLiquido(), "operadorLiquido");
            validarOperador(dto.getOperadorCusto(), "operadorCusto");
            rubrica.setOperadorBruto(dto.getOperadorBruto());
            rubrica.setOperadorLiquido(dto.getOperadorLiquido());
            rubrica.setOperadorCusto(dto.getOperadorCusto());
            return;
        }
        aplicarOperadoresDeTipo(rubrica);
    }

    private void aplicarOperadoresDeTipo(Rubrica rubrica) {
        if (rubrica.getTipoRubrica() == null || rubrica.getTipoRubrica().getDescricao() == null) {
            rubrica.setOperadorBruto((short) 0);
            rubrica.setOperadorLiquido((short) 0);
            rubrica.setOperadorCusto((short) 0);
            return;
        }
        switch (rubrica.getTipoRubrica().getDescricao().toUpperCase()) {
            case "PROVENTO" -> {
                rubrica.setOperadorBruto((short) 1);
                rubrica.setOperadorLiquido((short) 1);
                rubrica.setOperadorCusto((short) 1);
            }
            case "DESCONTO" -> {
                rubrica.setOperadorBruto((short) 0);
                rubrica.setOperadorLiquido((short) -1);
                rubrica.setOperadorCusto((short) 0);
            }
            default -> {
                rubrica.setOperadorBruto((short) 0);
                rubrica.setOperadorLiquido((short) 0);
                rubrica.setOperadorCusto((short) 0);
            }
        }
    }

    private void validarOperador(Short operador, String campo) {
        if (operador == null || !OPERADORES_VALIDOS.contains(operador)) {
            throw new IllegalArgumentException(campo + " deve ser -1, 0 ou 1");
        }
    }
}
