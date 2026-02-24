package com.example.Repository.Custom;

import com.example.DTOs.QuadraDashboardDTO;
import com.example.Models.Quadra;

import java.util.List;
import java.util.Optional;

public interface QuadraRepositoryCustom {
    Quadra salvarComSchema(Quadra quadra, String schema);
    List<Quadra> listarTodasComSchema(String schema);

    List<Quadra> findByAtivoTrue(String schema);

    Optional<Quadra> buscarPorIdComSchema(Integer id, String schema);
    void alterarStatusComSchema(Integer id, String schema);
    List<Quadra> listarAtivasComSchema(String schema);
    List<QuadraDashboardDTO> findEstatisticasQuadras(String schema);
}
