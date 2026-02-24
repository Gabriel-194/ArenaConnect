package com.example.Repository;

import com.example.Models.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByUserIdOrderByDataCriacaoDesc(Long userId);
    long countByUserIdAndLidaFalse(Long userId);
}