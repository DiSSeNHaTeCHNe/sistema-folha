package br.com.techne.sistemafolha.dashboard.infrastructure;

import br.com.techne.sistemafolha.dashboard.domain.DashboardLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardLayoutRepository extends JpaRepository<DashboardLayout, Long> {

    Optional<DashboardLayout> findByUsuarioId(Long usuarioId);

    void deleteByUsuarioId(Long usuarioId);
}
