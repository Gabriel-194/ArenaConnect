package com.example.Repository.Custom;

import com.example.DTOs.QuadraDashboardDTO;
import com.example.Models.Quadra;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuadraRepositoryImpl implements QuadraRepositoryCustom {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private void definirSchema(String schema) {
        if (schema == null || schema.isEmpty()) schema = "public";
        entityManager.createNativeQuery("SET search_path TO " + schema).executeUpdate();
    }

    @Override
    public Quadra salvarComSchema(Quadra quadra, String schema) {
        definirSchema(schema);
        if (quadra.getId() == null) {
            entityManager.persist(quadra);
            return quadra;
        } else {
            return entityManager.merge(quadra);
        }
    }

    @Override
    public List<Quadra> listarTodasComSchema(String schema) {
        definirSchema(schema);
        return entityManager.createQuery("SELECT q FROM Quadra q", Quadra.class).getResultList();
    }

    @Override
    public Optional<Quadra> buscarPorIdComSchema(Integer id, String schema) {
        definirSchema(schema);
        Quadra quadra = entityManager.find(Quadra.class, id);
        return Optional.ofNullable(quadra);
    }

    @Override
    public void alterarStatusComSchema(Integer id, String schema) {
        definirSchema(schema);
        entityManager.createQuery("UPDATE Quadra q SET q.ativo = CASE WHEN q.ativo = true THEN false ELSE true END WHERE q.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public List<Quadra> listarAtivasComSchema(String schema) {
        String sql = "SELECT * FROM " + schema + ".quadras WHERE ativo = true";
        Query query = entityManager.createNativeQuery(sql, Quadra.class);
        return query.getResultList();
    }

    @Override
    public List<Quadra> findByAtivoTrue(String schema) {

        String sql = "SELECT * FROM " + schema + ".quadras WHERE ativo = true";

        Query query = entityManager.createNativeQuery(sql, Quadra.class);

        return query.getResultList();
    }

    @Override
    public List<QuadraDashboardDTO> findEstatisticasQuadras(String schema) {
        String sql = "SELECT q.id_quadra as id, q.nome as nome, COUNT(a.id_agendamento) as jogos " +
                "FROM " + schema + ".quadras q " +
                "LEFT JOIN " + schema + ".agendamentos a ON q.id_quadra = a.id_quadra " +
                "AND a.status != 'CANCELADO' " +
                "GROUP BY q.id_quadra, q.nome " +
                "ORDER BY jogos DESC";

        return jdbcTemplate.query(sql, new DataClassRowMapper<>(QuadraDashboardDTO.class));
    }

}

