package com.example.Scheduler;

import com.example.Models.AgendamentoHistorico;
import com.example.Repository.HistoricoRepository;
import com.example.Service.AgendamentoService;
import com.example.Service.AsaasService;
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

    @Scheduled(cron = "0 0 3 * * ?")
    public void reconciliarPagamentosPendentes(){
        List<AgendamentoHistorico> pendentes = historicoRepository.findByStatus("PENDENTE");

        for(AgendamentoHistorico historico : pendentes){
            if (historico.getAsaasPaymentId() == null) continue;

            if (historico.getDataInicio().isAfter(LocalDateTime.now().minusMinutes(5))) continue;

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


}
