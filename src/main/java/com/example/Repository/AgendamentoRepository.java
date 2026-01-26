package com.example.Repository;

import com.example.Models.Agendamentos;
import com.example.Models.Arena;
import com.example.Repository.Custom.AgendamentoRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamentos, Integer> , AgendamentoRepositoryCustom {


}
