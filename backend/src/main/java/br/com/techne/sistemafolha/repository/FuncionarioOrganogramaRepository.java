package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.FuncionarioOrganograma;
import br.com.techne.sistemafolha.model.NoOrganograma;
import br.com.techne.sistemafolha.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioOrganogramaRepository extends JpaRepository<FuncionarioOrganograma, Long> {
    
    // Consultas por nó do organograma
    List<FuncionarioOrganograma> findByNoOrganograma(NoOrganograma noOrganograma);
    
    @Query("SELECT fo FROM FuncionarioOrganograma fo JOIN FETCH fo.funcionario f WHERE fo.noOrganograma = :noOrganograma AND f.ativo = true")
    List<FuncionarioOrganograma> findByNoOrganogramaWithFuncionarioAtivo(@Param("noOrganograma") NoOrganograma noOrganograma);
    
    // Consultas por funcionário
    List<FuncionarioOrganograma> findByFuncionario(Funcionario funcionario);
    
    @Query("SELECT fo FROM FuncionarioOrganograma fo JOIN FETCH fo.noOrganograma n WHERE fo.funcionario = :funcionario AND n.ativo = true")
    List<FuncionarioOrganograma> findByFuncionarioWithNoAtivo(@Param("funcionario") Funcionario funcionario);
    
    // Verificar se já existe associação
    boolean existsByFuncionarioAndNoOrganograma(Funcionario funcionario, NoOrganograma noOrganograma);
    
    Optional<FuncionarioOrganograma> findByFuncionarioAndNoOrganograma(Funcionario funcionario, NoOrganograma noOrganograma);
    
    // Remover associação
    @Modifying
    @Transactional
    void deleteByFuncionarioAndNoOrganograma(Funcionario funcionario, NoOrganograma noOrganograma);
    
    // Remover todas as associações de um nó
    @Modifying
    @Transactional
    void deleteByNoOrganograma(NoOrganograma noOrganograma);
    
    // Remover todas as associações de um funcionário
    @Modifying
    @Transactional
    void deleteByFuncionario(Funcionario funcionario);
    
    // Consulta para organograma ativo
    @Query("SELECT fo FROM FuncionarioOrganograma fo JOIN FETCH fo.funcionario f JOIN FETCH fo.noOrganograma n WHERE n.organogramaAtivo = true AND n.ativo = true AND f.ativo = true")
    List<FuncionarioOrganograma> findByOrganogramaAtivo();
    
    // Contar funcionários por nó
    long countByNoOrganograma(NoOrganograma noOrganograma);
    
    @Query("SELECT COUNT(fo) FROM FuncionarioOrganograma fo WHERE fo.noOrganograma = :noOrganograma AND fo.funcionario.ativo = true")
    long countByNoOrganogramaWithFuncionarioAtivo(@Param("noOrganograma") NoOrganograma noOrganograma);
} 