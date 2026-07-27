package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.cadastros.domain.TipoRubrica;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.RubricaRepository;
import br.com.techne.sistemafolha.cadastros.infrastructure.TipoRubricaRepository;
import br.com.techne.sistemafolha.cadastros.port.FuncionarioImportRef;
import br.com.techne.sistemafolha.cadastros.port.RubricaImportRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CadastrosImportLookupAdapterTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private RubricaRepository rubricaRepository;

    @Mock
    private TipoRubricaRepository tipoRubricaRepository;

    @InjectMocks
    private CadastrosImportLookupAdapter adapter;

    @Test
    void findFuncionarioByIdExterno_presente_retornaRef() {
        Funcionario funcionario = funcionario(1L, "EXT001", 100L);
        when(funcionarioRepository.findByIdExterno("EXT001")).thenReturn(Optional.of(funcionario));

        Optional<FuncionarioImportRef> result = adapter.findFuncionarioByIdExterno("EXT001");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
        assertEquals("EXT001", result.get().idExterno());
        assertEquals(100L, result.get().centroCustoId());
    }

    @Test
    void findFuncionarioByIdExterno_ausente_retornaEmpty() {
        when(funcionarioRepository.findByIdExterno("INEXISTENTE")).thenReturn(Optional.empty());

        Optional<FuncionarioImportRef> result = adapter.findFuncionarioByIdExterno("INEXISTENTE");

        assertTrue(result.isEmpty());
    }

    @Test
    void findOrCreateRubrica_existente_naoCria() {
        Rubrica rubrica = rubricaExistente(10L, "0010", "PROVENTO");
        when(rubricaRepository.findByCodigo("0010")).thenReturn(Optional.of(rubrica));

        RubricaImportRef result = adapter.findOrCreateRubrica("0010", "Salário Base", "PROVENTO");

        assertEquals(10L, result.id());
        assertEquals("0010", result.codigo());
        assertEquals("PROVENTO", result.tipoRubricaDescricao());
        verify(rubricaRepository, never()).save(any());
    }

    @Test
    void findOrCreateRubrica_nova_criaComTipo() {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao("DESCONTO");

        when(rubricaRepository.findByCodigo("0020")).thenReturn(Optional.empty());
        when(tipoRubricaRepository.findByDescricao("DESCONTO")).thenReturn(Optional.of(tipo));
        when(rubricaRepository.save(any(Rubrica.class))).thenAnswer(inv -> {
            Rubrica r = inv.getArgument(0);
            r.setId(20L);
            return r;
        });

        RubricaImportRef result = adapter.findOrCreateRubrica("0020", "INSS", "DESCONTO");

        assertEquals(20L, result.id());
        assertEquals("0020", result.codigo());
        assertEquals("DESCONTO", result.tipoRubricaDescricao());
        verify(rubricaRepository).save(any(Rubrica.class));
    }

    @Test
    void countFuncionariosAtivos_retornaTotal() {
        when(funcionarioRepository.countByAtivoTrue()).thenReturn(42L);

        assertEquals(42L, adapter.countFuncionariosAtivos());
    }

    @Test
    void countFuncionariosAtivosPorCentros_filtraPorCentro() {
        Funcionario f1 = funcionario(1L, "E1", 100L);
        Funcionario f2 = funcionario(2L, "E2", 200L);
        Funcionario f3 = funcionario(3L, "E3", 100L);

        when(funcionarioRepository.findByAtivoTrue()).thenReturn(List.of(f1, f2, f3));

        long count = adapter.countFuncionariosAtivosPorCentros(Set.of(100L));

        assertEquals(2L, count);
    }

    @Test
    void countFuncionariosAtivosPorCentros_setVazio_retornaZero() {
        assertEquals(0L, adapter.countFuncionariosAtivosPorCentros(Set.of()));
    }

    private Funcionario funcionario(Long id, String idExterno, Long centroCustoId) {
        LinhaNegocio linhaNegocio = new LinhaNegocio();
        linhaNegocio.setId(10L);

        CentroCusto centroCusto = new CentroCusto();
        centroCusto.setId(centroCustoId);
        centroCusto.setLinhaNegocio(linhaNegocio);

        Cargo cargo = new Cargo();
        cargo.setId(5L);

        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        funcionario.setIdExterno(idExterno);
        funcionario.setNome("Func " + id);
        funcionario.setCpf("12345678901");
        funcionario.setCentroCusto(centroCusto);
        funcionario.setCargo(cargo);
        return funcionario;
    }

    private Rubrica rubricaExistente(Long id, String codigo, String tipoDescricao) {
        TipoRubrica tipo = new TipoRubrica();
        tipo.setDescricao(tipoDescricao);

        Rubrica rubrica = new Rubrica();
        rubrica.setId(id);
        rubrica.setCodigo(codigo);
        rubrica.setTipoRubrica(tipo);
        return rubrica;
    }
}
