package com.example.Repository.Custom;

import com.example.Models.Agendamentos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepositoryCustom {

    Agendamentos salvarComSchema(Agendamentos agendamento,String schema);

    List<Agendamentos> findAgendamentosDoDiaComSchema(Integer idQuadra, LocalDateTime inicio, LocalDateTime fim, String schema);

    List<Agendamentos> findAllAgendamentos (Integer idQuadra, LocalDate data, String schema);

    List<Agendamentos> findAgendamentosClients(Integer idUser);

}
