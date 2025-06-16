package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.CargoDTO;
import br.com.techne.sistemafolha.exception.CargoNotFoundException;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.model.Cargo;
import br.com.techne.sistemafolha.model.LinhaNegocio;
import br.com.techne.sistemafolha.repository.CargoRepository;
import br.com.techne.sistemafolha.repository.LinhaNegocioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CargoService {
    private final CargoRepository cargoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;

    public List<CargoDTO> listarTodas() {
        return cargoRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CargoDTO> listarPorLinhaNegocio(Long linhaNegocioId) {
        return cargoRepository.findByLinhaNegocioIdAndAtivoTrue(linhaNegocioId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public CargoDTO buscarPorId(Long id) {
        return cargoRepository.findById(id)
                .filter(Cargo::getAtivo)
                .map(this::toDTO)
                .orElseThrow(() -> new CargoNotFoundException(id));
    }

    @Transactional
    public CargoDTO cadastrar(CargoDTO dto) {
        if (cargoRepository.existsByCodigoAndAtivoTrue(dto.codigo())) {
            throw new IllegalArgumentException("Já existe um cargo ativo com este código");
        }

        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
                .filter(ln -> ln.isAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));

        Cargo cargo = toEntity(dto);
        cargo.setLinhaNegocio(linhaNegocio);
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public CargoDTO atualizar(Long id, CargoDTO dto) {
        Cargo cargo = cargoRepository.findById(id)
                .filter(Cargo::getAtivo)
                .orElseThrow(() -> new CargoNotFoundException(id));

        if (!cargo.getCodigo().equals(dto.codigo()) && 
            cargoRepository.existsByCodigoAndAtivoTrue(dto.codigo())) {
            throw new IllegalArgumentException("Já existe um cargo ativo com este código");
        }

        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(dto.linhaNegocioId())
                .filter(ln -> ln.isAtivo())
                .orElseThrow(() -> new LinhaNegocioNotFoundException(dto.linhaNegocioId()));

        cargo.setCodigo(dto.codigo());
        cargo.setDescricao(dto.descricao());
        cargo.setLinhaNegocio(linhaNegocio);
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public void remover(Long id) {
        Cargo cargo = cargoRepository.findById(id)
                .filter(Cargo::getAtivo)
                .orElseThrow(() -> new CargoNotFoundException(id));
        cargo.setAtivo(false);
        cargoRepository.save(cargo);
    }

    private CargoDTO toDTO(Cargo cargo) {
        return new CargoDTO(
            cargo.getId(),
            cargo.getCodigo(),
            cargo.getDescricao(),
            cargo.getAtivo(),
            cargo.getLinhaNegocio().getId()
        );
    }

    private Cargo toEntity(CargoDTO dto) {
        Cargo cargo = new Cargo();
        cargo.setCodigo(dto.codigo());
        cargo.setDescricao(dto.descricao());
        cargo.setAtivo(true);
        return cargo;
    }
} 