package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.NoOrganograma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoOrganogramaRepository extends JpaRepository<NoOrganograma, Long> {
    
    // Consultas para nós ativos
    List<NoOrganograma> findByAtivoTrue();
    
    Optional<NoOrganograma> findByIdAndAtivoTrue(Long id);
    
    // Consultas para organograma ativo
    List<NoOrganograma> findByOrganogramaAtivoTrueAndAtivoTrue();
    
    Optional<NoOrganograma> findByOrganogramaAtivoTrue();
    
    // Consultas hierárquicas
    List<NoOrganograma> findByParentAndAtivoTrueOrderByPosicao(NoOrganograma parent);
    
    List<NoOrganograma> findByParentIsNullAndAtivoTrueOrderByPosicao();
    
    @Query("SELECT n FROM NoOrganograma n WHERE n.parent.id = :parentId AND n.ativo = true ORDER BY n.posicao")
    List<NoOrganograma> findByParentIdAndAtivoTrueOrderByPosicao(@Param("parentId") Long parentId);
    
    // Consulta para obter árvore completa do organograma ativo
    @Query("""
        SELECT n FROM NoOrganograma n 
        LEFT JOIN FETCH n.funcionarios f 
        LEFT JOIN FETCH f.funcionario func
        LEFT JOIN FETCH n.centrosCusto c 
        LEFT JOIN FETCH c.centroCusto cc
        WHERE n.organogramaAtivo = true AND n.ativo = true 
        ORDER BY n.nivel, n.posicao
        """)
    List<NoOrganograma> findOrganogramaAtivoComAssociacoes();
    
    // Consulta para verificar se um nó tem filhos
    boolean existsByParentAndAtivoTrue(NoOrganograma parent);
    
    // Consulta para obter o maior nível na hierarquia
    @Query("SELECT MAX(n.nivel) FROM NoOrganograma n WHERE n.organogramaAtivo = true AND n.ativo = true")
    Optional<Integer> findMaxNivelOrganogramaAtivo();
    
    // Consulta para obter nós por nível
    List<NoOrganograma> findByNivelAndOrganogramaAtivoTrueAndAtivoTrueOrderByPosicao(Integer nivel);
    
    // Soft delete
    @Modifying
    @Transactional
    @Query("UPDATE NoOrganograma n SET n.ativo = false WHERE n.id = :id")
    void softDelete(@Param("id") Long id);
    
    // Soft delete em cascata será implementado programaticamente no service
    
    // Ativar organograma (desativa todos os outros)
    @Modifying
    @Transactional
    @Query("UPDATE NoOrganograma n SET n.organogramaAtivo = false WHERE n.organogramaAtivo = true")
    void desativarTodosOrganogramas();
    
    @Modifying
    @Transactional
    @Query("UPDATE NoOrganograma n SET n.organogramaAtivo = true WHERE n.id = :id")
    void ativarOrganograma(@Param("id") Long id);
    
    // Contar nós filhos
    long countByParentAndAtivoTrue(NoOrganograma parent);
    
    // Verificar se existe organograma ativo
    boolean existsByOrganogramaAtivoTrue();
} 