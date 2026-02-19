package com.example.Repository;

import com.example.Models.AgendamentoHistorico;
import com.example.Models.Agendamentos;
import com.example.Models.Arena;
import com.example.Repository.Custom.AgendamentoRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamentos, Integer> , AgendamentoRepositoryCustom {

    @Modifying
    @Transactional
    @Query("UPDATE Agendamentos a SET a.status = 'FINALIZADO' WHERE a.id_agendamento IN :ids")
    void finalizarAgendamentosPorIds(@Param("ids") List<Integer> ids);

    @Modifying
    @Transactional
    @Query("UPDATE Agendamentos a SET a.status = 'CANCELADO' WHERE a.id_agendamento IN :ids")
    void cancelarAgendamentosPorIds(@Param("ids") List<Integer> ids);


}

