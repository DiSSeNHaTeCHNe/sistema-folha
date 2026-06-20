package br.com.techne.sistemafolha.repository;

import br.com.techne.sistemafolha.model.RefreshToken;
import br.com.techne.sistemafolha.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    List<RefreshToken> findByUsuario(Usuario usuario);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.usuario = :usuario")
    void deleteByUsuario(@Param("usuario") Usuario usuario);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.dataExpiracao < :agora")
    void deleteByDataExpiracaoBefore(@Param("agora") LocalDateTime agora);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revogado = true WHERE rt.usuario = :usuario")
    void revogarTodosPorUsuario(@Param("usuario") Usuario usuario);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revogado = true WHERE rt.token = :token")
    void revogarPorToken(@Param("token") String token);
} 