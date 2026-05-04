package com.example.Scheduler;

import com.example.Models.AgendamentoHistorico;
import com.example.Multitenancy.TenantContext;
import com.example.Repository.AgendamentoRepository;
import com.example.Repository.ArenaRepository;
import com.example.Repository.HistoricoRepository;
import com.example.Service.AsaasService;
import com.example.Service.NotificacaoService;
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

    @Autowired
    private NotificacaoService notificacaoService;

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void finishedBookings() {
        LocalDateTime now = LocalDateTime.now();

        List<AgendamentoHistorico> vencidosGlobal = historicoRepository.findJogosVencidosGlobalmente(now);

        if (vencidosGlobal.isEmpty()) return;

        // 🔧 Otimização: Agrupa por arena uma única vez — O(V)
        // O Map já contém tanto IDs quanto os objetos, eliminando re-filtros O(V) por arena
        Map<Integer, List<AgendamentoHistorico>> porArena = vencidosGlobal.stream()
                .filter(a -> a.getId_arena() != null)
                .filter(a -> a.getIdAgendamento() != null)
                .collect(Collectors.groupingBy(AgendamentoHistorico::getId_arena));

        for (Map.Entry<Integer, List<AgendamentoHistorico>> entry : porArena.entrySet()) {
            Integer id_Arena = entry.getKey();
            List<AgendamentoHistorico> agendamentosDestaArena = entry.getValue();

            // 🔧 Extrai IDs direto do agrupamento — O(1) lookup ao invés de O(V) filtro
            List<Integer> idsParaFinalizar = agendamentosDestaArena.stream()
                    .map(AgendamentoHistorico::getIdAgendamento)
                    .collect(Collectors.toList());

            if (id_Arena == null || id_Arena.equals(0)) continue;

            try {
                String schema = arenaRepository.findSchemaNameById(id_Arena.longValue());
                TenantContext.setCurrentTenant(schema);

                agendamentoRepository.finalizarAgendamentosPorIds(idsParaFinalizar, schema);

                // 🔧 Usa a lista já agrupada — antes re-filtrava vencidosGlobal inteiro
                for (AgendamentoHistorico hist : agendamentosDestaArena) {
                    if (hist.getIdUser() != null) {
                        notificacaoService.enviar(
                                hist.getIdUser().longValue(),
                                "Reserva Finalizada",
                                "Sua reserva foi Finalizada.",
                                "FINALIZADO"
                        );
                    }
                }

                logger.info("✅ Arena {}: Finalizados {} jogos (IDs: {})", id_Arena, idsParaFinalizar.size(), idsParaFinalizar);
            } catch (Exception e) {
                logger.error("❌ Erro ao finalizar jogos na arena {}: {}", id_Arena, e.getMessage());
            } finally {
                TenantContext.clear();
            }

            try {
                TenantContext.setCurrentTenant("public");
                historicoRepository.atualizarStatusEmLoteManual(idsParaFinalizar, id_Arena, "FINALIZADO");
            } catch (Exception e) {
                logger.error("Erro ao atualizar jogos finalizados na tabela do public");
            }
        }
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cancelarReservasNaoPagas() {
        LocalDateTime limite = LocalDateTime.now().plusMinutes(30);

        List<AgendamentoHistorico> pendentes = historicoRepository.findPendentesParaCancelar(limite);

        if (pendentes.isEmpty()) return;

        // 🔧 Otimização: Agrupa uma única vez e usa o agrupamento para tudo
        Map<Integer, List<AgendamentoHistorico>> porSchema = pendentes.stream()
                .filter(a -> a.getId_arena() != null)
                .filter(a -> a.getIdAgendamento() != null)
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

                agendamentoRepository.cancelarAgendamentosPorIds(idsParaCancelar, schema);

                // 🔧 Usa listaHistorico já agrupada — antes re-filtrava pendentes inteiro
                for (AgendamentoHistorico hist : listaHistorico) {
                    if (hist.getIdUser() != null) {
                        notificacaoService.enviar(
                                hist.getIdUser().longValue(),
                                "Reserva Cancelada",
                                "Sua reserva foi cancelada por falta de pagamento.",
                                "CANCELADO"
                        );
                    }
                }
                logger.info("❌ Arena {}: Cancelados {} agendamentos por falta de pagamento.", schema, idsParaCancelar.size());

            } catch (Exception e) {
                logger.error("Erro ao cancelar na arena {}", schema, e);
            } finally {
                TenantContext.clear();
            }

            // 🔧 Cancelamento Asaas — filtra apenas os que têm paymentId
            for (AgendamentoHistorico h : listaHistorico) {
                if (h.getAsaasPaymentId() != null) {
                    try {
                        asaasService.cancelarCobranca(h.getAsaasPaymentId());
                    } catch (Exception e) {
                        logger.warn("Erro ao cancelar cobrança Asaas {}: {}", h.getAsaasPaymentId(), e.getMessage());
                    }
                }
            }

            try {
                TenantContext.setCurrentTenant("public");
                historicoRepository.atualizarStatusEmLoteManual(idsParaCancelar, id_arena, "CANCELADO");
            } catch (Exception e) {
                logger.error("Erro ao atualizar histórico cancelado", e);
            }
        }
    }
}
