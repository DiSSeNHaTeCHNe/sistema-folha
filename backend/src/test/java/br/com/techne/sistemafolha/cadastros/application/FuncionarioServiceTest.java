package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.FuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.api.FuncionarioStatusFiltro;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.infrastructure.CargoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.CentroCustoRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;
    @Mock
    private CargoRepository cargoRepository;
    @Mock
    private CentroCustoRepository centroCustoRepository;

    @InjectMocks
    private FuncionarioService funcionarioService;

    @Test
    void cadastrar_rejeita_cpf_ativo_duplicado() {
        FuncionarioDTO dto = dtoBase("12345678901", "MAT001");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("12345678901")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> funcionarioService.cadastrar(dto));

        assertEquals("Já existe um funcionário ativo com este CPF", ex.getMessage());
        verify(funcionarioRepository, never()).save(any());
    }

    @Test
    void cadastrar_rejeita_id_externo_duplicado() {
        FuncionarioDTO dto = dtoBase("12345678901", "MAT001");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("12345678901")).thenReturn(false);
        when(funcionarioRepository.existsByIdExterno("MAT001")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> funcionarioService.cadastrar(dto));

        assertEquals("Já existe um funcionário com este ID externo (matrícula)", ex.getMessage());
    }

    @Test
    void cadastrar_permite_mesmo_cpf_quando_nao_ha_ativo() {
        FuncionarioDTO dto = dtoBase("12345678901", "MAT002");
        when(funcionarioRepository.existsByCpfAndAtivoTrue("12345678901")).thenReturn(false);
        when(funcionarioRepository.existsByIdExterno("MAT002")).thenReturn(false);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargoAtivo()));
        when(centroCustoRepository.findById(1L)).thenReturn(Optional.of(centroCustoAtivo()));
        when(funcionarioRepository.save(any(Funcionario.class))).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        FuncionarioDTO result = funcionarioService.cadastrar(dto);

        assertEquals(2L, result.id());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    void listar_default_ativo_passes_ativo_true_to_repository() {
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true)))
                .thenReturn(Collections.emptyList());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.ATIVO);

        verify(funcionarioRepository).findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(true));
    }

    @Test
    void listar_inativo_passes_ativo_false_to_repository() {
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(Collections.emptyList());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.INATIVO);

        verify(funcionarioRepository).findByFiltros(isNull(), isNull(), isNull(), isNull(), eq(false));
    }

    @Test
    void listar_todos_passes_ativo_null_to_repository() {
        when(funcionarioRepository.findByFiltros(isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Collections.emptyList());

        funcionarioService.listar(null, null, null, null, FuncionarioStatusFiltro.TODOS);

        verify(funcionarioRepository).findByFiltros(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void listar_with_nome_and_status_combined() {
        when(funcionarioRepository.findByFiltros(eq("%Maria%"), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(List.of());

        funcionarioService.listar("Maria", null, null, null, FuncionarioStatusFiltro.INATIVO);

        verify(funcionarioRepository).findByFiltros(eq("%Maria%"), isNull(), isNull(), isNull(), eq(false));
    }

    @Test
    void remover_sets_ativo_false() {
        Funcionario funcionario = funcionarioAtivo();
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(funcionarioRepository.save(funcionario)).thenReturn(funcionario);

        funcionarioService.remover(1L);

        assertFalse(funcionario.getAtivo());
        verify(funcionarioRepository).save(funcionario);
    }

    @Test
    void segundo_remover_throws_funcionario_not_found_exception() {
        Funcionario funcionario = funcionarioAtivo();
        funcionario.setAtivo(false);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        assertThrows(FuncionarioNotFoundException.class, () -> funcionarioService.remover(1L));
        verify(funcionarioRepository, never()).save(any());
    }

    private Funcionario funcionarioAtivo() {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("Maria Teste");
        funcionario.setCpf("12345678901");
        funcionario.setDataAdmissao(LocalDate.of(2024, 1, 15));
        funcionario.setCargo(cargoAtivo());
        funcionario.setCentroCusto(centroCustoAtivo());
        funcionario.setAtivo(true);
        return funcionario;
    }

    private FuncionarioDTO dtoBase(String cpf, String idExterno) {
        return new FuncionarioDTO(
                null,
                "Maria Teste",
                cpf,
                LocalDate.of(2024, 1, 15),
                1L,
                "Analista",
                1L,
                "TI",
                1L,
                "Software",
                idExterno,
                true
        );
    }

    private Cargo cargoAtivo() {
        Cargo cargo = new Cargo();
        cargo.setId(1L);
        cargo.setDescricao("Analista");
        cargo.setAtivo(true);
        return cargo;
    }

    private CentroCusto centroCustoAtivo() {
        LinhaNegocio ln = new LinhaNegocio();
        ln.setId(1L);
        ln.setDescricao("Software");
        CentroCusto cc = new CentroCusto();
        cc.setId(1L);
        cc.setDescricao("TI");
        cc.setAtivo(true);
        cc.setLinhaNegocio(ln);
        return cc;
    }
}
