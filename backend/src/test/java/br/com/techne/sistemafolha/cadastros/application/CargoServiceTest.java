package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.CargoDTO;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CargoNotFoundException;
import br.com.techne.sistemafolha.cadastros.infrastructure.CargoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CargoServiceTest {

    @Mock
    private CargoRepository cargoRepository;

    @InjectMocks
    private CargoService cargoService;

    @Test
    void listarTodos_retornaApenasAtivos() {
        Cargo ativo = cargo(1L, "Analista", true);
        Cargo inativo = cargo(2L, "Estagiário", false);
        when(cargoRepository.findAll()).thenReturn(List.of(ativo, inativo));

        List<CargoDTO> result = cargoService.listarTodos();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Analista", result.get(0).descricao());
    }

    @Test
    void buscarPorId_retornaDtoQuandoAtivo() {
        Cargo cargo = cargo(1L, "Analista", true);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargo));

        CargoDTO result = cargoService.buscarPorId(1L);

        assertEquals(1L, result.id());
        assertEquals("Analista", result.descricao());
        assertEquals(true, result.ativo());
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoInativo() {
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargo(1L, "Analista", false)));

        assertThrows(CargoNotFoundException.class, () -> cargoService.buscarPorId(1L));
    }

    @Test
    void buscarPorId_lancaExcecaoQuandoNaoEncontrado() {
        when(cargoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CargoNotFoundException.class, () -> cargoService.buscarPorId(99L));
    }

    @Test
    void cadastrar_persisteCargoAtivo() {
        CargoDTO dto = new CargoDTO(null, "Desenvolvedor", null);
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(inv -> {
            Cargo cargo = inv.getArgument(0);
            cargo.setId(10L);
            return cargo;
        });

        CargoDTO result = cargoService.cadastrar(dto);

        assertEquals(10L, result.id());
        assertEquals("Desenvolvedor", result.descricao());
        assertEquals(true, result.ativo());
        verify(cargoRepository).save(any(Cargo.class));
    }

    @Test
    void atualizar_alteraDescricao() {
        Cargo cargo = cargo(1L, "Analista", true);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargo));
        when(cargoRepository.save(cargo)).thenReturn(cargo);

        CargoDTO dto = new CargoDTO(1L, "Analista Sênior", true);
        CargoDTO result = cargoService.atualizar(1L, dto);

        assertEquals("Analista Sênior", result.descricao());
        assertEquals("Analista Sênior", cargo.getDescricao());
        verify(cargoRepository).save(cargo);
    }

    @Test
    void atualizar_lancaExcecaoQuandoNaoEncontrado() {
        when(cargoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CargoNotFoundException.class,
            () -> cargoService.atualizar(99L, new CargoDTO(99L, "Cargo", true)));
        verify(cargoRepository, never()).save(any());
    }

    @Test
    void remover_desativaCargo() {
        Cargo cargo = cargo(1L, "Analista", true);
        when(cargoRepository.findById(1L)).thenReturn(Optional.of(cargo));
        when(cargoRepository.save(cargo)).thenReturn(cargo);

        cargoService.remover(1L);

        assertFalse(cargo.isAtivo());
        verify(cargoRepository).save(cargo);
    }

    @Test
    void remover_lancaExcecaoQuandoNaoEncontrado() {
        when(cargoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CargoNotFoundException.class, () -> cargoService.remover(99L));
        verify(cargoRepository, never()).save(any());
    }

    private Cargo cargo(Long id, String descricao, boolean ativo) {
        Cargo cargo = new Cargo();
        cargo.setId(id);
        cargo.setDescricao(descricao);
        cargo.setAtivo(ativo);
        return cargo;
    }
}
