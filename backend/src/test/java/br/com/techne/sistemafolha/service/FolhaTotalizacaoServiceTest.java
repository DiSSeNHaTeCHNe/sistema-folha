package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.model.BeneficioMensal;
import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.Rubrica;
import br.com.techne.sistemafolha.model.TipoRubrica;
import br.com.techne.sistemafolha.repository.BeneficioMensalRepository;
import br.com.techne.sistemafolha.repository.BeneficioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaTotalizacaoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private BeneficioRepository beneficioRepository;

    @Mock
    private BeneficioMensalRepository beneficioMensalRepository;

    @InjectMocks
    private FolhaTotalizacaoService folhaTotalizacaoService;

    @Test
    void calcularTotaisPorFuncionario_quandoBeneficioMensalExiste_usaSomenteMensal() {
        Funcionario funcionario = funcionario(1L, "Ana Silva");
        FolhaPagamento linha = linhaFolha(funcionario, new BigDecimal("8000.00"));

        BeneficioMensal vr = beneficioMensal(funcionario, new BigDecimal("500.00"));
        BeneficioMensal vt = beneficioMensal(funcionario, new BigDecimal("200.00"));
        Beneficio legado = beneficioLegado(funcionario, new BigDecimal("999.00"));

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(true);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                1L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(vr, vt));

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
                .calcularTotaisPorFuncionario(List.of(linha));

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("8000.00"), total.salCustoFolha());
        assertEquals(new BigDecimal("700.00"), total.salCustoBeneficios());
        assertEquals(new BigDecimal("8700.00"), total.salCustoTechne());
        assertEquals(2, total.totalBeneficios());

        verify(beneficioRepository, never()).findAtivosByFuncionarioAndPeriodo(any(), any(), any());
        verify(beneficioMensalRepository).findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                eq(1L), eq(COMPETENCIA_INICIO), eq(COMPETENCIA_FIM));
    }

    @Test
    void calcularTotaisPorFuncionario_quandoBeneficioMensalNaoExiste_usaLegado() {
        Funcionario funcionario = funcionario(2L, "Bruno Costa");
        FolhaPagamento linha = linhaFolha(funcionario, new BigDecimal("6000.00"));

        Beneficio legado = beneficioLegado(funcionario, new BigDecimal("350.00"));

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(false);
        when(beneficioRepository.findAtivosByFuncionarioAndPeriodo(
                2L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(legado));

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
                .calcularTotaisPorFuncionario(List.of(linha));

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(new BigDecimal("6000.00"), total.salCustoFolha());
        assertEquals(new BigDecimal("350.00"), total.salCustoBeneficios());
        assertEquals(new BigDecimal("6350.00"), total.salCustoTechne());
        assertEquals(1, total.totalBeneficios());

        verify(beneficioMensalRepository, never())
                .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any(), any());
        verify(beneficioRepository).findAtivosByFuncionarioAndPeriodo(
                2L, COMPETENCIA_INICIO, COMPETENCIA_FIM);
    }

    @Test
    void calcularTotaisPorFuncionario_quandoMensalExisteMasFuncionarioSemLancamento_custoBeneficiosZero() {
        Funcionario funcionario = funcionario(3L, "Carla Dias");
        FolhaPagamento linha = linhaFolha(funcionario, new BigDecimal("5000.00"));

        when(beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(true);
        when(beneficioMensalRepository.findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                3L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of());

        List<FolhaTotaisFuncionarioDTO> resultado = folhaTotalizacaoService
                .calcularTotaisPorFuncionario(List.of(linha));

        assertEquals(1, resultado.size());
        FolhaTotaisFuncionarioDTO total = resultado.get(0);
        assertEquals(BigDecimal.ZERO.setScale(2), total.salCustoBeneficios());
        assertEquals(new BigDecimal("5000.00"), total.salCustoTechne());
        assertEquals(0, total.totalBeneficios());

        verify(beneficioRepository, never()).findAtivosByFuncionarioAndPeriodo(any(), any(), any());
    }

    private Funcionario funcionario(Long id, String nome) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome(nome);
        return funcionario;
    }

    private FolhaPagamento linhaFolha(Funcionario funcionario, BigDecimal valor) {
        TipoRubrica tipoProvento = new TipoRubrica();
        tipoProvento.setDescricao("PROVENTO");

        Rubrica rubrica = new Rubrica();
        rubrica.setTipoRubrica(tipoProvento);

        FolhaPagamento linha = new FolhaPagamento();
        linha.setFuncionario(funcionario);
        linha.setRubrica(rubrica);
        linha.setValor(valor);
        linha.setDataInicio(COMPETENCIA_INICIO);
        linha.setDataFim(COMPETENCIA_FIM);
        return linha;
    }

    private BeneficioMensal beneficioMensal(Funcionario funcionario, BigDecimal valor) {
        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setFuncionario(funcionario);
        beneficio.setValor(valor);
        beneficio.setCompetenciaInicio(COMPETENCIA_INICIO);
        beneficio.setCompetenciaFim(COMPETENCIA_FIM);
        beneficio.setAtivo(true);
        return beneficio;
    }

    private Beneficio beneficioLegado(Funcionario funcionario, BigDecimal valor) {
        Beneficio beneficio = new Beneficio();
        beneficio.setFuncionario(funcionario);
        beneficio.setValor(valor);
        beneficio.setDataInicio(COMPETENCIA_INICIO);
        beneficio.setDataFim(COMPETENCIA_FIM);
        beneficio.setAtivo(true);
        return beneficio;
    }
}
