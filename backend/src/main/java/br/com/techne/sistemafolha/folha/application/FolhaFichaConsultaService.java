package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioLinhaSnapshot;
import br.com.techne.sistemafolha.folha.api.FichaLinhaDetalheDTO;
import br.com.techne.sistemafolha.folha.api.Totalizador;
import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FichaMensal;
import br.com.techne.sistemafolha.folha.domain.FichaMensalNotFoundException;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.shared.access.CentroCustoEfetivo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolhaFichaConsultaService {

    private static final String ORIGEM_BENEFICIO = "BENEFICIO";

    private final FichaMensalRepository fichaMensalRepository;
    private final FichaLinhaRepository fichaLinhaRepository;
    private final BeneficioConsultaPort beneficioConsultaPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final UsuarioLookupPort usuarioLookupPort;

    @Transactional(readOnly = true)
    public List<FichaLinhaDetalheDTO> listarLinhasPorTotalizador(
            String login, Long fichaMensalId, Totalizador totalizador) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        FichaMensal ficha = fichaMensalRepository.findByIdAtivoWithFuncionario(fichaMensalId)
            .orElseThrow(() -> new FichaMensalNotFoundException(fichaMensalId));

        if (!podeAcessarFicha(ficha, contexto)) {
            throw new FichaMensalNotFoundException(fichaMensalId);
        }

        FolhaMotorCalculo.Totalizador motorTotalizador = toMotorTotalizador(totalizador);
        List<FichaLinhaDetalheDTO> detalhes = new ArrayList<>();

        for (FichaLinha linha : fichaLinhaRepository.findByFichaMensalIdAndAtivoTrue(fichaMensalId)) {
            short operador = operadorDe(linha, motorTotalizador);
            if (operador == 0) {
                continue;
            }
            FolhaMotorCalculo.LinhaCalculoInput input = toInput(linha);
            BigDecimal contribuicao = FolhaMotorCalculo.contribuicao(input, motorTotalizador);
            if (contribuicao.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            detalhes.add(new FichaLinhaDetalheDTO(
                linha.getValor(),
                contribuicao,
                origemLinha(linha.getOrigemLinha()),
                linha.getRubrica().getCodigo(),
                linha.getRubrica().getDescricao(),
                linha.getPorcentagem()
            ));
        }

        if (totalizador == Totalizador.COMPANY_COST) {
            Long funcionarioId = ficha.getFuncionario().getId();
            List<BeneficioLinhaSnapshot> beneficios = beneficioConsultaPort.findLinhasPorFuncionarioECompetencia(
                funcionarioId, ficha.getCompetenciaInicio(), ficha.getCompetenciaFim());
            for (BeneficioLinhaSnapshot beneficio : beneficios) {
                BigDecimal valor = beneficio.valor() != null ? beneficio.valor() : BigDecimal.ZERO;
                if (valor.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                detalhes.add(new FichaLinhaDetalheDTO(
                    valor,
                    FolhaMotorCalculo.arredondar(valor),
                    ORIGEM_BENEFICIO,
                    beneficio.tipoCodigo(),
                    beneficio.tipoDescricao(),
                    null
                ));
            }
        }

        detalhes.sort(Comparator.comparing(FichaLinhaDetalheDTO::rubricaCodigo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return detalhes;
    }

    @Transactional(readOnly = true)
    public Long buscarFichaIdPorFuncionario(
            String login,
            Long funcionarioId,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            boolean decimoTerceiro) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        FichaMensal ficha = fichaMensalRepository.findByFuncionarioAndCompetencia(
                funcionarioId, competenciaInicio, competenciaFim, decimoTerceiro)
            .orElseThrow(() -> new FichaMensalNotFoundException(funcionarioId));

        if (!podeAcessarFicha(ficha, contexto)) {
            throw new FichaMensalNotFoundException(funcionarioId);
        }

        return ficha.getId();
    }

    boolean podeAcessarFicha(FichaMensal ficha, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        Long linhaCcId = ficha.getCentroCusto() != null ? ficha.getCentroCusto().getId() : null;
        Long funcCcId = ficha.getFuncionario() != null && ficha.getFuncionario().getCentroCusto() != null
            ? ficha.getFuncionario().getCentroCusto().getId() : null;
        return CentroCustoEfetivo.pertenceAoEscopo(
            CentroCustoEfetivo.idOf(linhaCcId, funcCcId), contexto.centrosCustoIds());
    }

    private AccessContextDTO obterContextoAcesso(String login) {
        Usuario usuario = usuarioLookupPort.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return organogramaAcessoPort.obterContextoAcesso(usuario.getId());
    }

    private FolhaMotorCalculo.Totalizador toMotorTotalizador(Totalizador totalizador) {
        return switch (totalizador) {
            case GROSS -> FolhaMotorCalculo.Totalizador.GROSS;
            case NET -> FolhaMotorCalculo.Totalizador.NET;
            case COMPANY_COST -> FolhaMotorCalculo.Totalizador.COMPANY_COST;
        };
    }

    private short operadorDe(FichaLinha linha, FolhaMotorCalculo.Totalizador totalizador) {
        return switch (totalizador) {
            case GROSS -> linha.getOperadorBruto() != null ? linha.getOperadorBruto() : 0;
            case NET -> linha.getOperadorLiquido() != null ? linha.getOperadorLiquido() : 0;
            case COMPANY_COST -> linha.getOperadorCusto() != null ? linha.getOperadorCusto() : 0;
        };
    }

    private FolhaMotorCalculo.LinhaCalculoInput toInput(FichaLinha linha) {
        return new FolhaMotorCalculo.LinhaCalculoInput(
            linha.getValor(),
            linha.getOperadorBruto() != null ? linha.getOperadorBruto() : 0,
            linha.getOperadorLiquido() != null ? linha.getOperadorLiquido() : 0,
            linha.getOperadorCusto() != null ? linha.getOperadorCusto() : 0,
            linha.getPorcentagem()
        );
    }

    private String origemLinha(OrigemLinha origem) {
        return origem != null ? origem.name() : OrigemLinha.FOLHA_ADP.name();
    }
}
