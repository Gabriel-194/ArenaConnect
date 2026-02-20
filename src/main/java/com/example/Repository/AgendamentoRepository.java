package com.example.Repository;


import com.example.Models.Agendamentos;

import com.example.Repository.Custom.AgendamentoRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AgendamentoRepository extends JpaRepository<Agendamentos, Integer> , AgendamentoRepositoryCustom {
}

