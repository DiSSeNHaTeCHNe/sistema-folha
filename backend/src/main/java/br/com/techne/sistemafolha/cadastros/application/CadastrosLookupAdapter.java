package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.LinhaNegocioRepository;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CadastrosLookupAdapter implements CadastrosLookupPort {

    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;

    @Override
    public Optional<CentroCusto> findCentroCustoById(Long id) {
        return centroCustoRepository.findById(id);
    }

    @Override
    public Optional<LinhaNegocio> findLinhaNegocioById(Long id) {
        return linhaNegocioRepository.findById(id);
    }
}
