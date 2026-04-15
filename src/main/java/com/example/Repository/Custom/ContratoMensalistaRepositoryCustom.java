package com.example.Repository.Custom;

import com.example.Models.ContratoMensalista;

import java.util.List;

// Interface customizada para queries com controle explícito de schema (multi-tenant)
public interface ContratoMensalistaRepositoryCustom {

    List<ContratoMensalista> findByIdUserComSchema(Integer idUser, String schema);
}
