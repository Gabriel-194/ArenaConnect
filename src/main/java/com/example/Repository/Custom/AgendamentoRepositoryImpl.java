package com.example.Repository.Custom;

import com.example.Models.Agendamentos;
import com.example.Models.Quadra;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AgendamentoRepositoryImpl implements AgendamentoRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private void definirSchema(String schema) {
        if (schema == null || schema.isEmpty()) schema = "public";
        entityManager.createNativeQuery("SET search_path TO " + schema).executeUpdate();
    }


    @Override
    public Agendamentos salvarComSchema(Agendamentos agendamento, String schema) {
        definirSchema(schema);
        if (agendamento.getId_agendamento() == null) {
            entityManager.persist(agendamento);
            return agendamento;
        } else {
            return entityManager.merge(agendamento);
        }
    }

    @Override
    public List<Agendamentos> findAgendamentosDoDiaComSchema(Integer idQuadra, LocalDateTime inicio, LocalDateTime fim, String schema) {

            definirSchema(schema);

            String jpql = "SELECT a FROM Agendamentos a WHERE a.id_quadra = :idQuadra " +
                    "AND a.data_inicio >= :inicio AND a.data_inicio < :fim " +
                    "AND a.status <> 'CANCELADO'";

            TypedQuery<Agendamentos> query = entityManager.createQuery(jpql, Agendamentos.class);
            query.setParameter("idQuadra", idQuadra);
            query.setParameter("inicio", inicio);
            query.setParameter("fim", fim);

            return query.getResultList();
    }

    @Override
    public List<Agendamentos> findAllAgendamentos(Integer idQuadra, LocalDate data, String schema) {
        definirSchema(schema);

        StringBuilder jpql = new StringBuilder("SELECT a FROM Agendamentos a WHERE 1=1 ");

        if (idQuadra != null) {
            jpql.append(" AND a.id_quadra = :idQuadra ");
        }

        if (data != null) {
            jpql.append(" AND a.data_inicio >= :inicio AND a.data_inicio < :fim ");
        }
        jpql.append(" ORDER BY a.data_inicio ASC");

        TypedQuery<Agendamentos> query = entityManager.createQuery(jpql.toString(), Agendamentos.class);

        if (idQuadra != null) {
            query.setParameter("idQuadra", idQuadra);
        }
        if (data != null) {
            query.setParameter("inicio", data.atStartOfDay());
            query.setParameter("fim", data.plusDays(1).atStartOfDay());
        }

        return query.getResultList();
    }

    @Override
    public Optional<Agendamentos> buscarPorIdComSchema(Integer id, String schema) {
        definirSchema(schema);
        Agendamentos agendamento = entityManager.find(Agendamentos.class, id);

        return Optional.ofNullable(agendamento);
    }
}
