package br.com.techne.sistemafolha.repository;

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
public interface CentroCustoRepository extends JpaRepository<CentroCusto, Long> {
    boolean existsByDescricao(String descricao);

    @Modifying
    @Transactional
    @Query("UPDATE CentroCusto c SET c.ativo = false WHERE c.id = :id")
    void softDelete(@Param("id") Long id);

    List<CentroCusto> findByAtivoTrue();
    List<CentroCusto> findByLinhaNegocioIdAndAtivoTrue(Long linhaNegocioId);
} 