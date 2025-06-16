package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.Funcionario;
import br.com.techne.sistemafolha.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    List<Funcionario> findByCentroCustoAndAtivoTrue(CentroCusto centroCusto);
    Optional<Funcionario> findByIdAndAtivoTrue(Long id);
    
    @Modifying
    @Transactional
    @Query("UPDATE Funcionario f SET f.ativo = false WHERE f.id = :id")
    void softDelete(@Param("id") Long id);

    Optional<Funcionario> findByIdExterno(String idExterno);
    boolean existsByIdExterno(String idExterno);

    List<Funcionario> findByAtivoTrue();
    Optional<Funcionario> findByCpfAndAtivoTrue(String cpf);
    boolean existsByCpfAndAtivoTrue(String cpf);
} 