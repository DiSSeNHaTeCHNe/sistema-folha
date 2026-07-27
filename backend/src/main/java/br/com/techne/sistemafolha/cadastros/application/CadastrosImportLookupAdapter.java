package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.TipoRubricaRepository;
import br.com.techne.sistemafolha.cadastros.port.CadastrosImportLookupPort;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioImportRef;
import br.com.techne.sistemafolha.cadastros.port.RubricaImportRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CadastrosImportLookupAdapter implements CadastrosImportLookupPort {

    private final FuncionarioRepository funcionarioRepository;
    private final RubricaRepository rubricaRepository;
    private final TipoRubricaRepository tipoRubricaRepository;

    @Override
    public Optional<FuncionarioImportRef> findFuncionarioByIdExterno(String idExterno) {
        return funcionarioRepository.findByIdExterno(idExterno)
            .map(this::toFuncionarioImportRef);
    }

    @Override
    @Transactional
    public RubricaImportRef findOrCreateRubrica(String codigo, String descricao, String tipoRubricaDescricao) {
        Rubrica rubrica = rubricaRepository.findByCodigo(codigo)
            .orElseGet(() -> {
                Rubrica novaRubrica = new Rubrica();
                novaRubrica.setCodigo(codigo);
                novaRubrica.setDescricao(descricao);
                novaRubrica.setTipoRubrica(obterTipoRubrica(tipoRubricaDescricao));
                novaRubrica.setPorcentagem(100.0);
                return rubricaRepository.save(novaRubrica);
            });
        return toRubricaImportRef(rubrica);
    }

    @Override
    public long countFuncionariosAtivos() {
        return funcionarioRepository.countByAtivoTrue();
    }

    @Override
    public long countFuncionariosAtivosPorCentros(Set<Long> centrosCustoIds) {
        if (centrosCustoIds == null || centrosCustoIds.isEmpty()) {
            return 0L;
        }
        return funcionarioRepository.findByAtivoTrue().stream()
            .filter(f -> f.getCentroCusto() != null && centrosCustoIds.contains(f.getCentroCusto().getId()))
            .count();
    }

    private TipoRubrica obterTipoRubrica(String tipoRubricaDescricao) {
        return tipoRubricaRepository.findByDescricao(tipoRubricaDescricao)
            .orElseThrow(() -> new RuntimeException("Tipo de rubrica " + tipoRubricaDescricao + " não encontrado"));
    }

    private FuncionarioImportRef toFuncionarioImportRef(Funcionario funcionario) {
        Long cargoId = funcionario.getCargo() != null ? funcionario.getCargo().getId() : null;
        Long centroCustoId = funcionario.getCentroCusto() != null ? funcionario.getCentroCusto().getId() : null;
        Long linhaNegocioId = null;
        if (funcionario.getCentroCusto() != null && funcionario.getCentroCusto().getLinhaNegocio() != null) {
            linhaNegocioId = funcionario.getCentroCusto().getLinhaNegocio().getId();
        }
        return new FuncionarioImportRef(
            funcionario.getId(),
            funcionario.getIdExterno(),
            funcionario.getNome(),
            funcionario.getCpf(),
            cargoId,
            centroCustoId,
            linhaNegocioId
        );
    }

    private RubricaImportRef toRubricaImportRef(Rubrica rubrica) {
        return new RubricaImportRef(
            rubrica.getId(),
            rubrica.getCodigo(),
            rubrica.getTipoRubrica().getDescricao()
        );
    }
}
