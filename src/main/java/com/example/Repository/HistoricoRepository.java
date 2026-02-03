package com.example.Repository;

import com.example.Models.AgendamentoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface HistoricoRepository extends JpaRepository<AgendamentoHistorico, Integer> {

    @Query("SELECT h FROM AgendamentoHistorico h WHERE h.idAgendamento = :idOrigem AND h.schemaName = :schemaName")
    Optional<AgendamentoHistorico> buscarPorOrigem(@Param("idOrigem") Integer idOrigem,
                                                   @Param("schemaName") String schemaName);

    @Query("SELECT h FROM AgendamentoHistorico h WHERE h.idUser = :idUser ORDER BY h.dataInicio DESC")
    List<AgendamentoHistorico> buscarHistoricoPorUsuario(@Param("idUser") Integer idUser);

    Optional<AgendamentoHistorico> findBySchemaNameAndIdAgendamento(String schemaName, Integer id_agendamento);
}
