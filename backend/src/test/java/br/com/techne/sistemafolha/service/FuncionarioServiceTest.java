package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.FuncionarioDTO;
import br.com.techne.sistemafolha.model.Cargo;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.LinhaNegocio;
import br.com.techne.sistemafolha.repository.CargoRepository;
import br.com.techne.sistemafolha.repository.CentroCustoRepository;
import br.com.techne.sistemafolha.repository.FuncionarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
