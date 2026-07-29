package br.com.techne.sistemafolha.cadastros.application;

import br.com.techne.sistemafolha.cadastros.api.CargoDTO;
import br.com.techne.sistemafolha.cadastros.domain.CargoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.infrastructure.CargoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CargoService {
    private static final Logger logger = LoggerFactory.getLogger(CargoService.class);
    private static final String DOMAIN = "cadastros";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);

    private final CargoRepository cargoRepository;

    public List<CargoDTO> listarTodos() {
        logger.info("{}Listando todos os cargos", DOMAIN_PREFIX);
        return cargoRepository.findAll().stream()
                .filter(c -> c.isAtivo())
                .map(this::toDTO)
                .toList();
    }

    public CargoDTO buscarPorId(Long id) {
        logger.info("{}Buscando cargo por ID: {}", DOMAIN_PREFIX, id);
        return cargoRepository.findById(id)
                .filter(c -> c.isAtivo())
                .map(this::toDTO)
                .orElseThrow(() -> new CargoNotFoundException(id));
    }

    @Transactional
    public CargoDTO cadastrar(CargoDTO dto) {
        logger.info("{}Cadastrando novo cargo: {}", DOMAIN_PREFIX, dto.descricao());
        Cargo cargo = toEntity(dto);
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public CargoDTO atualizar(Long id, CargoDTO dto) {
        logger.info("{}Atualizando cargo ID: {}", DOMAIN_PREFIX, id);
        Cargo cargo = cargoRepository.findById(id)
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(id));

        cargo.setDescricao(dto.descricao());
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public void remover(Long id) {
        logger.info("{}Removendo cargo ID: {}", DOMAIN_PREFIX, id);
        Cargo cargo = cargoRepository.findById(id)
                .filter(c -> c.isAtivo())
                .orElseThrow(() -> new CargoNotFoundException(id));
        cargo.setAtivo(false);
        cargoRepository.save(cargo);
    }

    private CargoDTO toDTO(Cargo cargo) {
        return new CargoDTO(
            cargo.getId(),
            cargo.getDescricao(),
            cargo.isAtivo()
        );
    }

    private Cargo toEntity(CargoDTO dto) {
        Cargo cargo = new Cargo();
        cargo.setDescricao(dto.descricao());
        cargo.setAtivo(true);
        return cargo;
    }
} 