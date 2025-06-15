package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    List<Funcionario> findByCentroCustoAndAtivoTrue(String centroCusto);
    Optional<Funcionario> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Query("UPDATE Funcionario f SET f.ativo = false WHERE f.id = :id")
    void softDelete(@Param("id") Long id);

    Optional<Funcionario> findByIdExterno(String idExterno);
    boolean existsByIdExterno(String idExterno);
} 