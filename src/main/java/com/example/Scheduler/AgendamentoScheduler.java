package com.example.Scheduler;

import com.example.Models.AgendamentoHistorico;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Service.AsaasService;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
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

    @Scheduled(fixedRate = 60000)
    public void finishedBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<AgendamentoHistorico> vencidosGlobal = historicoRepository.findJogosVencidosGlobalmente(now);

        if (vencidosGlobal.isEmpty()) return;

        logger.info("⏰ Encontrados {} jogos para finalizar no total.", vencidosGlobal.size());

        Map<String, List<Integer>> gamesSchema = vencidosGlobal.stream()
                .collect(Collectors.groupingBy(
                        AgendamentoHistorico::getSchemaName,
                        Collectors.mapping(AgendamentoHistorico::getIdAgendamento,Collectors.toList())
                ));

        for (Map.Entry<String,List<Integer>> entry : gamesSchema.entrySet()){
            String schema = entry.getKey();
            List<Integer> idsParaFinalizar = entry.getValue();

            if(schema == null || schema.equals("public")) continue;

            try{
                TenantContext.setCurrentTenant(schema);

                agendamentoRepository.finalizarAgendamentosPorIds(idsParaFinalizar);
                logger.info("✅ Arena {}: Finalizados {} jogos (IDs: {})", schema, idsParaFinalizar.size(), idsParaFinalizar);
            } catch (Exception e) {
            logger.error("❌ Erro ao finalizar jogos na arena {}: {}", schema, e.getMessage());
            } finally {
            TenantContext.clear();
            }

            try{
                historicoRepository.atualizarStatusEmLoteManual(idsParaFinalizar,schema,"FINALIZADO");
            } catch (Exception e){
                logger.error("Erro ao atualizar jogos finalizados na tabela do public");
            }
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cancelarReservasNaoPagas(){
        LocalDateTime limite = LocalDateTime.now().plusMinutes(30);

        List<AgendamentoHistorico> pendentes = historicoRepository.findPendentesParaCancelar(limite);

        if (pendentes.isEmpty()) return;

        Map<String, List<AgendamentoHistorico>> porSchema = pendentes.stream()
                .collect(Collectors.groupingBy(AgendamentoHistorico::getSchemaName));

        for (Map.Entry<String, List<AgendamentoHistorico>> entry : porSchema.entrySet()) {
            String schema = entry.getKey();
            List<AgendamentoHistorico> listaHistorico = entry.getValue();

            List<Integer> idsParaCancelar = listaHistorico.stream()
                    .map(AgendamentoHistorico::getIdAgendamento)
                    .collect(Collectors.toList());

            if (schema == null || schema.equals("public")) continue;

            try {
                TenantContext.setCurrentTenant(schema);

                agendamentoRepository.cancelarAgendamentosPorIds(idsParaCancelar);
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

                historicoRepository.atualizarStatusEmLoteManual(idsParaCancelar, schema, "CANCELADO");
            } catch (Exception e) {
                logger.error("Erro ao atualizar histórico cancelado", e);
            }
        }
    }
}
