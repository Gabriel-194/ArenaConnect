package com.example.Scheduler;

import com.example.Models.AgendamentoHistorico;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Service.AsaasService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AgendamentoScheduler {
    private static final Logger logger = LoggerFactory.getLogger(AgendamentoScheduler.class);

    @Autowired
    private ArenaRepository arenaRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private HistoricoRepository historicoRepository;

    @Autowired
    private AsaasService asaasService;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void finishedBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<AgendamentoHistorico> vencidosGlobal = historicoRepository.findJogosVencidosGlobalmente(now);

        if (vencidosGlobal.isEmpty()) return;

        logger.info("⏰ Encontrados {} jogos para finalizar no total.", vencidosGlobal.size());

        Map<Integer, List<Integer>> gamesSchema = vencidosGlobal.stream()
                .collect(Collectors.groupingBy(
                        AgendamentoHistorico::getId_arena,
                        Collectors.mapping(AgendamentoHistorico::getIdAgendamento,Collectors.toList())
                ));

        for (Map.Entry<Integer,List<Integer>> entry : gamesSchema.entrySet()){
            Integer id_Arena = entry.getKey();
            List<Integer> idsParaFinalizar = entry.getValue();

            if(id_Arena == null || id_Arena.equals(0)) continue;

            try{

                String schema = arenaRepository.findSchemaNameById(id_Arena.longValue());
                TenantContext.setCurrentTenant(schema);

                agendamentoRepository.finalizarAgendamentosPorIds(idsParaFinalizar,schema);
                logger.info("✅ Arena {}: Finalizados {} jogos (IDs: {})", id_Arena, idsParaFinalizar.size(), idsParaFinalizar);
            } catch (Exception e) {
            logger.error("❌ Erro ao finalizar jogos na arena {}: {}", id_Arena, e.getMessage());
            } finally {
            TenantContext.clear();
            }

            try{
                historicoRepository.atualizarStatusEmLoteManual(idsParaFinalizar, id_Arena,"FINALIZADO");
            } catch (Exception e){
                logger.error("Erro ao atualizar jogos finalizados na tabela do public");
            }
        }
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cancelarReservasNaoPagas(){
        LocalDateTime limite = LocalDateTime.now().plusMinutes(30);

        List<AgendamentoHistorico> pendentes = historicoRepository.findPendentesParaCancelar(limite);

        if (pendentes.isEmpty()) return;

        Map<Integer, List<AgendamentoHistorico>> porSchema = pendentes.stream()
                .collect(Collectors.groupingBy(AgendamentoHistorico::getId_arena));

        for (Map.Entry<Integer, List<AgendamentoHistorico>> entry : porSchema.entrySet()) {
            Integer id_arena = entry.getKey();
            List<AgendamentoHistorico> listaHistorico = entry.getValue();

            List<Integer> idsParaCancelar = listaHistorico.stream()
                    .map(AgendamentoHistorico::getIdAgendamento)
                    .collect(Collectors.toList());

            String schema = arenaRepository.findSchemaNameById(id_arena.longValue());

            if (schema == null || schema.equals("public")) continue;

            try {
                TenantContext.setCurrentTenant(schema);

                agendamentoRepository.cancelarAgendamentosPorIds(idsParaCancelar,schema);
                logger.info("❌ Arena {}: Cancelados {} agendamentos por falta de pagamento.", schema, idsParaCancelar.size());

            } catch (Exception e) {
                logger.error("Erro ao cancelar na arena {}", schema, e);
            } finally {
                TenantContext.clear();
            }

            for (AgendamentoHistorico h : listaHistorico) {
                if (h.getAsaasPaymentId() != null) {
                    asaasService.cancelarCobranca(h.getAsaasPaymentId());
                }
            }

            try {

                historicoRepository.atualizarStatusEmLoteManual(idsParaCancelar, id_arena, "CANCELADO");
            } catch (Exception e) {
                logger.error("Erro ao atualizar histórico cancelado", e);
            }
        }
    }
}
