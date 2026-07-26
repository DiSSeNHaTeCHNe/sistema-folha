package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.BeneficioMensalDTO;
import br.com.techne.sistemafolha.dto.BeneficioMensalResumoDTO;
import br.com.techne.sistemafolha.exception.BeneficioMensalNotFoundException;
import br.com.techne.sistemafolha.model.BeneficioMensal;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.TipoBeneficio;
import br.com.techne.sistemafolha.repository.BeneficioMensalRepository;
import br.com.techne.sistemafolha.repository.BeneficioMensalResumoProjection;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import br.com.techne.sistemafolha.repository.TipoBeneficioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeneficioMensalServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private BeneficioMensalRepository beneficioMensalRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private TipoBeneficioRepository tipoBeneficioRepository;

    @InjectMocks
    private BeneficioMensalService beneficioMensalService;

    @Test
    void listarPorCompetencia_acesso_total_usa_query_sem_filtro_centro() {
        BeneficioMensal beneficio = beneficioAtivo(1L);
        when(beneficioMensalRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Collections.emptySet());

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        verify(beneficioMensalRepository).findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM);
        verify(beneficioMensalRepository, never())
                .findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue(
                        any(), any(), any());
    }

    @Test
    void listarPorCompetencia_acesso_restrito_usa_query_com_centros() {
        Set<Long> centros = Set.of(10L, 20L);
        BeneficioMensal beneficio = beneficioAtivo(2L);
        when(beneficioMensalRepository
                .findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).id());
        verify(beneficioMensalRepository)
                .findByCompetenciaInicioAndCompetenciaFimAndFuncionarioCentroCustoIdInAndAtivoTrue(
                        COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);
        verify(beneficioMensalRepository, never())
                .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(any(), any());
    }

    @Test
    void resumoPorCompetencia_acesso_total_usa_resumo_sem_filtro() {
        BeneficioMensalResumoProjection projection = resumoProjection(
                "VALE_REFEICAO", "Vale Refeição - Custo Empresa", new BigDecimal("1500.00"), 3L);
        when(beneficioMensalRepository.resumoPorCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(projection));

        List<BeneficioMensalResumoDTO> result = beneficioMensalService.resumoPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, Collections.emptySet());

        assertEquals(1, result.size());
        assertEquals("VALE_REFEICAO", result.get(0).codigo());
        assertEquals(new BigDecimal("1500.00"), result.get(0).total());
        assertEquals(3L, result.get(0).qtdLancamentos());
        verify(beneficioMensalRepository).resumoPorCompetencia(COMPETENCIA_INICIO, COMPETENCIA_FIM);
        verify(beneficioMensalRepository, never())
                .resumoPorCompetenciaAndCentroCustoIds(any(), any(), any());
    }

    @Test
    void resumoPorCompetencia_acesso_restrito_usa_resumo_com_centros() {
        Set<Long> centros = Set.of(10L);
        BeneficioMensalResumoProjection projection = resumoProjection(
                "SEGUROS", "Seguros - Custo Empresa", new BigDecimal("800.00"), 2L);
        when(beneficioMensalRepository.resumoPorCompetenciaAndCentroCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros))
                .thenReturn(List.of(projection));

        List<BeneficioMensalResumoDTO> result = beneficioMensalService.resumoPorCompetencia(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);

        assertEquals(1, result.size());
        assertEquals("SEGUROS", result.get(0).codigo());
        verify(beneficioMensalRepository).resumoPorCompetenciaAndCentroCustoIds(
                COMPETENCIA_INICIO, COMPETENCIA_FIM, centros);
        verify(beneficioMensalRepository, never()).resumoPorCompetencia(any(), any());
    }

    @Test
    void listarPorFuncionario_retorna_lancamentos_do_periodo() {
        BeneficioMensal beneficio = beneficioAtivo(5L);
        when(beneficioMensalRepository
                .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                        99L, COMPETENCIA_INICIO, COMPETENCIA_FIM))
                .thenReturn(List.of(beneficio));

        List<BeneficioMensalDTO> result = beneficioMensalService.listarPorFuncionario(
                99L, COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals(99L, result.get(0).funcionarioId());
    }

    @Test
    void criar_persiste_beneficio_ativo() {
        BeneficioMensalDTO dto = dtoBase(null, 1L, 2L, new BigDecimal("450.00"));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionarioAtivo(1L)));
        when(tipoBeneficioRepository.findById(2L)).thenReturn(Optional.of(tipoAtivo(2L, "VALE_REFEICAO")));
        when(beneficioMensalRepository.save(any(BeneficioMensal.class))).thenAnswer(inv -> {
            BeneficioMensal bm = inv.getArgument(0);
            bm.setId(10L);
            return bm;
        });

        BeneficioMensalDTO result = beneficioMensalService.criar(dto);

        assertEquals(10L, result.id());
        assertEquals("VALE_REFEICAO", result.tipoBeneficioCodigo());
        assertEquals(new BigDecimal("450.00"), result.valor());
        verify(beneficioMensalRepository).save(any(BeneficioMensal.class));
    }

    @Test
    void remover_desativa_beneficio() {
        BeneficioMensal beneficio = beneficioAtivo(7L);
        when(beneficioMensalRepository.findById(7L)).thenReturn(Optional.of(beneficio));
        when(beneficioMensalRepository.save(beneficio)).thenReturn(beneficio);

        beneficioMensalService.remover(7L);

        assertFalse(beneficio.getAtivo());
        verify(beneficioMensalRepository).save(beneficio);
    }

    @Test
    void remover_lanca_excecao_quando_nao_encontrado() {
        when(beneficioMensalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BeneficioMensalNotFoundException.class, () -> beneficioMensalService.remover(999L));
        verify(beneficioMensalRepository, never()).save(any());
    }

    private BeneficioMensalDTO dtoBase(Long id, Long funcionarioId, Long tipoBeneficioId, BigDecimal valor) {
        return new BeneficioMensalDTO(
                id,
                funcionarioId,
                null,
                tipoBeneficioId,
                null,
                null,
                null,
                null,
                valor,
                COMPETENCIA_INICIO,
                COMPETENCIA_FIM,
                null
        );
    }

    private BeneficioMensal beneficioAtivo(Long id) {
        BeneficioMensal beneficio = new BeneficioMensal();
        beneficio.setId(id);
        beneficio.setFuncionario(funcionarioAtivo(99L));
        beneficio.setTipoBeneficio(tipoAtivo(2L, "VALE_REFEICAO"));
        beneficio.setValor(new BigDecimal("450.00"));
        beneficio.setCompetenciaInicio(COMPETENCIA_INICIO);
        beneficio.setCompetenciaFim(COMPETENCIA_FIM);
        beneficio.setAtivo(true);
        return beneficio;
    }

    private Funcionario funcionarioAtivo(Long id) {
        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(10L);
        centroCusto.setDescricao("TI");

        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setNome("João Silva");
        funcionario.setCentroCusto(centroCusto);
        funcionario.setAtivo(true);
        return funcionario;
    }

    private TipoBeneficio tipoAtivo(Long id, String codigo) {
        TipoBeneficio tipo = new TipoBeneficio();
        tipo.setId(id);
        tipo.setCodigo(codigo);
        tipo.setDescricao("Vale Refeição - Custo Empresa");
        tipo.setAtivo(true);
        return tipo;
    }

    private BeneficioMensalResumoProjection resumoProjection(
            String codigo, String descricao, BigDecimal total, Long qtdLancamentos) {
        return new BeneficioMensalResumoProjection() {
            @Override
            public String getCodigo() {
                return codigo;
            }

            @Override
            public String getDescricao() {
                return descricao;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }

            @Override
            public Long getQtdLancamentos() {
                return qtdLancamentos;
            }
        };
    }
}
