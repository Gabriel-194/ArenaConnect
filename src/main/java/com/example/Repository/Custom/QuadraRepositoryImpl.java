package com.example.Repository.Custom;

import com.example.Models.Quadra;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.Optional;

public class QuadraRepositoryImpl implements QuadraRepositoryCustom {

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
        entityManager.createQuery("UPDATE Quadra q SET q.ativo = NOT q.ativo WHERE q.id = :id")
                .setParameter("id", id)
                .executeUpdate();
    }
}

