package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.infrastructure.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FuncionarioConsultaAdapterTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @InjectMocks
    private FuncionarioConsultaAdapter adapter;

    @Test
    void findById_funcionarioPresente_retornaOptionalComFuncionario() {
        Funcionario funcionario = funcionario(1L);
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));

        Optional<Funcionario> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void findById_funcionarioAusente_retornaOptionalVazio() {
        when(funcionarioRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Funcionario> result = adapter.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByCpfAndAtivoTrue_funcionarioPresente_retornaOptionalComFuncionario() {
        Funcionario funcionario = funcionario(2L);
        when(funcionarioRepository.findByCpfAndAtivoTrue("12345678901"))
            .thenReturn(Optional.of(funcionario));

        Optional<Funcionario> result = adapter.findByCpfAndAtivoTrue("12345678901");

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId());
    }

    @Test
    void findByCpfAndAtivoTrue_funcionarioAusente_retornaOptionalVazio() {
        when(funcionarioRepository.findByCpfAndAtivoTrue("00000000000"))
            .thenReturn(Optional.empty());

        Optional<Funcionario> result = adapter.findByCpfAndAtivoTrue("00000000000");

        assertTrue(result.isEmpty());
    }

    private Funcionario funcionario(Long id) {
        Funcionario funcionario = new Funcionario();
        funcionario.setId(id);
        return funcionario;
    }
}
