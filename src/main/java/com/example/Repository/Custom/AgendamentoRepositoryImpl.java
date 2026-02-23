package com.example.Repository.Custom;

import com.example.DTOs.AgendamentoDashboardDTO;
import com.example.DTOs.FaturamentoDTO;
import com.example.Models.Agendamentos;
import com.example.Models.Quadra;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<AgendamentoDashboardDTO> findAllDashboard(String schema) {

        definirSchema(schema);
        String sql = "SELECT id_agendamento, status FROM agendamentos";

        Query query = entityManager.createNativeQuery(sql);

        List<Object[]> results = query.getResultList();

        List<AgendamentoDashboardDTO> lista = new ArrayList<>();

        for (Object[] obj : results) {

            AgendamentoDashboardDTO dto = new AgendamentoDashboardDTO();

            dto.setIdAgendamento((Integer) obj[0]);
            dto.setStatus((String) obj[1]);

            lista.add(dto);
        }

        return lista;
    }

    @Override
    public void finalizarAgendamentosPorIds(List<Integer> ids, String schema) {
        if (ids == null || ids.isEmpty()) return;

        definirSchema(schema);
        String idsStr = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String sql = "UPDATE agendamentos SET status = 'FINALIZADO' WHERE id_agendamento IN (" + idsStr + ")";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Override
    public void cancelarAgendamentosPorIds(List<Integer> ids, String schema) {
        if (ids == null || ids.isEmpty()) return;

        definirSchema(schema);
        String idsStr = ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        String sql = "UPDATE agendamentos SET status = 'CANCELADO' WHERE id_agendamento IN (" + idsStr + ")";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    @Override
    public List<FaturamentoDTO> findFaturamentoAnual(String schema, int ano) {
        String sql = "SELECT EXTRACT(MONTH FROM a.data_inicio) as mes, SUM(a.valor_total) as total " +
                "FROM " + schema + ".agendamentos a " +
                "WHERE a.status IN ('FINALIZADO', 'CONFIRMADO') " +
                "AND EXTRACT(YEAR FROM a.data_inicio) = :ano " +
                "GROUP BY EXTRACT(MONTH FROM a.data_inicio) " +
                "ORDER BY mes";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("ano", ano);

        List<Object[]> resultados = query.getResultList();

        String[] nomesMeses = {"Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"};
        List<FaturamentoDTO> lista = new ArrayList<>();

        for (String nome : nomesMeses) {
            lista.add(new FaturamentoDTO(nome, 0.0));
        }

        for (Object[] row : resultados) {
            int mesIndex = ((Number) row[0]).intValue() - 1;
            double valorTotal = ((Number) row[1]).doubleValue();

            lista.get(mesIndex).setValor(valorTotal);
        }

        return lista;
    }


}
