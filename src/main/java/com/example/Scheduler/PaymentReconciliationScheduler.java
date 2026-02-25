package com.example.Scheduler;

import com.example.Models.AgendamentoHistorico;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Service.AgendamentoService;
import com.example.Service.AsaasService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaymentReconciliationScheduler {

    @Autowired
    private HistoricoRepository historicoRepository;

    @Autowired
    private AsaasService asaasService;

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private ArenaRepository arenaRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Transactional
    @Scheduled(fixedDelay = 3000000)
    public void reconciliarPagamentosPendentes(){
        List<AgendamentoHistorico> pendentes = historicoRepository.findByStatus("PENDENTE");

        for(AgendamentoHistorico historico : pendentes){
            if (historico.getAsaasPaymentId() == null) continue;

            try{
                String statusAsaas = asaasService.checkPaymentStatus(historico.getAsaasPaymentId());

                if(statusAsaas == null) continue;

                switch (statusAsaas){
                    case "CONFIRMED":
                    case "RECEIVED":
                    case "RECEIVED_IN_CASH":
                        agendamentoService.confirmPaymentWebhook(historico.getAsaasPaymentId());
                        break;
                }
            }catch (Exception e) {
                System.err.println("Erro ao reconciliar item " + historico.getId() + ": " + e.getMessage());
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void corrigirDivergenciasConfirmados() {
        List<AgendamentoHistorico> confirmados = historicoRepository.findByStatus("CONFIRMADO");

        for (AgendamentoHistorico historico : confirmados) {
            try {
                Integer idArena = historico.getId_arena();
                String schema = arenaRepository.findSchemaNameById(idArena.longValue());

                if (schema != null) {
                    agendamentoRepository.buscarPorIdComSchema(historico.getIdAgendamento(), schema).ifPresent(agendamento -> {

                        if (!"CONFIRMADO".equals(agendamento.getStatus())) {
                            System.out.println(" Auto-Cura: Divergência encontrada! Agendamento " + agendamento.getId_agendamento()
                                    + " no schema " + schema + " estava " + agendamento.getStatus() + ". Forçando para CONFIRMADO!");

                            agendamento.setStatus("CONFIRMADO");
                            agendamentoRepository.salvarComSchema(agendamento, schema);
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("Erro ao corrigir divergência do agendamento " + historico.getIdAgendamento() + ": " + e.getMessage());
            }
        }
    }
}