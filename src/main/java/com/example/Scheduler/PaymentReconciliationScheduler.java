package com.example.Scheduler;

import com.example.Models.AgendamentoHistorico;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Service.AgendamentoService;
import com.example.Service.AsaasService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PaymentReconciliationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReconciliationScheduler.class);
    private static final int MAX_API_CALLS_PER_RUN = 50;

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
    public void reconciliarPagamentosPendentes() {
        List<AgendamentoHistorico> pendentes = new ArrayList<>();
        pendentes.addAll(historicoRepository.findByStatus("PENDENTE"));
        pendentes.addAll(historicoRepository.findByStatus("MENSALISTA_PENDENTE"));

        Set<String> pagamentosPendentes = new LinkedHashSet<>();
        for (AgendamentoHistorico historico : pendentes) {
            if (historico.getAsaasPaymentId() != null && !historico.getAsaasPaymentId().isBlank()) {
                pagamentosPendentes.add(historico.getAsaasPaymentId());
            }
        }

        int processados = 0;
        for (String paymentId : pagamentosPendentes) {
            if (processados >= MAX_API_CALLS_PER_RUN) {
                logger.info("Reconciliacao: limite de {} chamadas atingido. {} restantes para a proxima execucao.",
                        MAX_API_CALLS_PER_RUN, pagamentosPendentes.size() - processados);
                break;
            }

            try {
                String statusAsaas = asaasService.checkPaymentStatus(paymentId);
                processados++;

                if (statusAsaas == null) continue;

                switch (statusAsaas) {
                    case "CONFIRMED":
                    case "RECEIVED":
                    case "RECEIVED_IN_CASH":
                        agendamentoService.confirmPaymentWebhook(paymentId);
                        logger.info("Reconciliacao: pagamento {} confirmado.", paymentId);
                        break;
                    case "REFUNDED":
                    case "REFUND_REQUESTED":
                    case "DELETED":
                        agendamentoService.cancelPaymentWebhook(paymentId, "RECONCILIACAO_" + statusAsaas);
                        logger.info("Reconciliacao: pagamento {} marcado como {}.", paymentId, statusAsaas);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                logger.warn("Erro ao reconciliar pagamento {}: {}", paymentId, e.getMessage());
            }
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 3000000)
    public void corrigirDivergenciasConfirmados() {
        corrigirDivergenciasPorStatus("CONFIRMADO");
        corrigirDivergenciasPorStatus("MENSALISTA_CONFIRMADO");
    }

    private void corrigirDivergenciasPorStatus(String statusConfirmado) {
        List<AgendamentoHistorico> confirmados = historicoRepository.findByStatus(statusConfirmado);

        int processados = 0;
        for (AgendamentoHistorico historico : confirmados) {
            if (processados >= MAX_API_CALLS_PER_RUN) break;

            try {
                Integer idArena = historico.getId_arena();
                if (idArena == null) continue;

                String schema = arenaRepository.findSchemaNameById(idArena.longValue());

                if (schema != null) {
                    agendamentoRepository.buscarPorIdComSchema(historico.getIdAgendamento(), schema).ifPresent(agendamento -> {
                        if (!statusConfirmado.equals(agendamento.getStatus())) {
                            logger.info("Auto-cura: agendamento {} no schema {} estava {}. Ajustando para {}.",
                                    agendamento.getId_agendamento(), schema, agendamento.getStatus(), statusConfirmado);

                            agendamento.setStatus(statusConfirmado);
                            agendamentoRepository.salvarComSchema(agendamento, schema);
                        }
                    });
                }
                processados++;
            } catch (Exception e) {
                logger.warn("Erro ao corrigir divergencia do agendamento {}: {}", historico.getIdAgendamento(), e.getMessage());
            }
        }
    }
}
