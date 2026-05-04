package com.example.Repository;

import com.example.Models.ContratoMensalista;
import com.example.Repository.Custom.ContratoMensalistaRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoMensalistaRepository extends JpaRepository<ContratoMensalista, Integer>, ContratoMensalistaRepositoryCustom {

    // Busca contratos de um usuário específico
    List<ContratoMensalista> findByIdUser(Integer idUser);

    // Busca todos os contratos ativos na arena atual
    List<ContratoMensalista> findByAtivoTrue();

    // Busca contratos pelo status (ex: PENDENTE)
    List<ContratoMensalista> findByStatus(String status);

    Optional<ContratoMensalista> findByAsaasPaymentId(String asaasPaymentId);
}
