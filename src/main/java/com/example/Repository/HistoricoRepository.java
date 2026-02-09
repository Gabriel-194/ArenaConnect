package com.example.Repository;

import com.example.Models.AgendamentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface HistoricoRepository extends JpaRepository<AgendamentoHistorico, Integer> {

    @Query("SELECT h FROM AgendamentoHistorico h WHERE h.idAgendamento = :idOrigem AND h.schemaName = :schemaName")
    Optional<AgendamentoHistorico> buscarPorOrigem(@Param("idOrigem") Integer idOrigem,
                                                   @Param("schemaName") String schemaName);

    @Query("SELECT h FROM AgendamentoHistorico h WHERE h.idUser = :idUser ORDER BY h.dataInicio DESC")
    List<AgendamentoHistorico> buscarHistoricoPorUsuario(@Param("idUser") Integer idUser);

    Optional<AgendamentoHistorico> findByAsaasPaymentId(String asaasPaymentId);

    @Query("SELECT h FROM AgendamentoHistorico h WHERE h.status = 'CONFIRMADO' AND h.data_fim < :agora")
    List<AgendamentoHistorico> findJogosVencidosGlobalmente(@Param("agora") LocalDateTime agora);

    @Modifying
    @Transactional
    @Query("UPDATE AgendamentoHistorico h SET h.status = :novoStatus WHERE h.idAgendamento IN :ids AND h.schemaName = :schema")
    void atualizarStatusEmLoteManual(@Param("ids") List<Integer> ids, @Param("schema") String schema, @Param("novoStatus") String novoStatus);

    @Query("SELECT h FROM AgendamentoHistorico h WHERE h.status = 'PENDENTE' AND h.dataInicio < :limiteTempo")
    List<AgendamentoHistorico> findPendentesParaCancelar(@Param("limiteTempo") LocalDateTime limiteTempo);
}
