package com.example.Repository.Custom;

import com.example.Models.Agendamentos;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepositoryCustom {

    Agendamentos salvarComSchema(Agendamentos agendamento,String schema);

    List<Agendamentos> findAgendamentosDoDiaComSchema(Integer idQuadra, LocalDateTime inicio, LocalDateTime fim, String schema);

}
