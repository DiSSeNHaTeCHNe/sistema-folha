package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.CentroCustoOrganograma;
import br.com.techne.sistemafolha.model.NoOrganograma;
import br.com.techne.sistemafolha.model.CentroCusto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CentroCustoOrganogramaRepository extends JpaRepository<CentroCustoOrganograma, Long> {
    
    // Consultas por nó do organograma
    List<CentroCustoOrganograma> findByNoOrganograma(NoOrganograma noOrganograma);
    
    @Query("SELECT cco FROM CentroCustoOrganograma cco JOIN FETCH cco.centroCusto cc WHERE cco.noOrganograma = :noOrganograma AND cc.ativo = true")
    List<CentroCustoOrganograma> findByNoOrganogramaWithCentroCustoAtivo(@Param("noOrganograma") NoOrganograma noOrganograma);
    
    // Consultas por centro de custo
    List<CentroCustoOrganograma> findByCentroCusto(CentroCusto centroCusto);
    
    @Query("SELECT cco FROM CentroCustoOrganograma cco JOIN FETCH cco.noOrganograma n WHERE cco.centroCusto = :centroCusto AND n.ativo = true")
    List<CentroCustoOrganograma> findByCentroCustoWithNoAtivo(@Param("centroCusto") CentroCusto centroCusto);
    
    // Verificar se já existe associação
    boolean existsByCentroCustoAndNoOrganograma(CentroCusto centroCusto, NoOrganograma noOrganograma);
    
    Optional<CentroCustoOrganograma> findByCentroCustoAndNoOrganograma(CentroCusto centroCusto, NoOrganograma noOrganograma);
    
    // Remover associação
    @Modifying
    @Transactional
    void deleteByCentroCustoAndNoOrganograma(CentroCusto centroCusto, NoOrganograma noOrganograma);
    
    // Remover todas as associações de um nó
    @Modifying
    @Transactional
    void deleteByNoOrganograma(NoOrganograma noOrganograma);
    
    // Remover todas as associações de um centro de custo
    @Modifying
    @Transactional
    void deleteByCentroCusto(CentroCusto centroCusto);
    
    // Consulta para organograma ativo
    @Query("SELECT cco FROM CentroCustoOrganograma cco JOIN FETCH cco.centroCusto cc JOIN FETCH cco.noOrganograma n WHERE n.organogramaAtivo = true AND n.ativo = true AND cc.ativo = true")
    List<CentroCustoOrganograma> findByOrganogramaAtivo();
    
    // Contar centros de custo por nó
    long countByNoOrganograma(NoOrganograma noOrganograma);
    
    @Query("SELECT COUNT(cco) FROM CentroCustoOrganograma cco WHERE cco.noOrganograma = :noOrganograma AND cco.centroCusto.ativo = true")
    long countByNoOrganogramaWithCentroCustoAtivo(@Param("noOrganograma") NoOrganograma noOrganograma);
} 