package com.example.Controller;

import com.example.Models.Arena;
import com.example.Models.Users;
import com.example.Repository.ArenaRepository;
import com.example.Repository.UserRepository;
import com.example.Service.AgendamentoService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private ArenaRepository arenaRepository;

    @Value("${asaas.api.key}")
    private String asaasApiKey;

    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping("/asaas")
    @Transactional
    public ResponseEntity<String> handleAsaasWebhook(@RequestBody String payload) {
        logger.info("📩 Webhook recebido! Processando...");

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            if (!root.has("event")) {
                return ResponseEntity.ok("Ignorado: Payload sem evento");
            }

            String event = root.get("event").asText();

            if ("PAYMENT_RECEIVED".equals(event) || "PAYMENT_CONFIRMED".equals(event)) {

                JsonNode paymentNode = root.path("payment");
                String paymentId = paymentNode.path("id").asText();

                String subscriptionId = "";
                if (paymentNode.hasNonNull("subscription")) {
                    subscriptionId = paymentNode.path("subscription").asText();
                }

                boolean isAgendamento = confirmPaymentifExist(paymentId);

                if (!isAgendamento && subscriptionId != null && !subscriptionId.isEmpty()) {
                    LocalDate novaDataExpiracao = LocalDate.now().plusMonths(1);
                    ativarArenaPorSubscription(subscriptionId, novaDataExpiracao);
                }

            }
            return ResponseEntity.ok("Webhook processado com sucesso");

        } catch (Exception e) {
            logger.error("❌ Erro ao processar webhook", e);
            return ResponseEntity.ok("Erro capturado");
        }
    }

    private void ativarArenaPorSubscription(String subscriptionId, LocalDate dataExpiracao) {

        Optional<Arena> arenaOpt = arenaRepository
                .findByAssasSubscriptionId(subscriptionId);

        if (arenaOpt.isPresent()) {

            Arena arena = arenaOpt.get();

            arena.setAtivo(true);
            arena.setDataExpiracao(dataExpiracao);

            arenaRepository.save(arena);

            logger.info(
                    "✅ Arena '{}' ativada até {}",
                    arena.getName(),
                    dataExpiracao
            );

        } else {
            logger.error(
                    "❌ Nenhuma arena encontrada para subscription: {}",
                    subscriptionId
            );
        }
    }

    public Boolean confirmPaymentifExist(String paymentId){
        if (paymentId == null || paymentId.isEmpty()) return false;

        boolean confirmado = agendamentoService.confirmPaymentWebhook(paymentId);

        if (confirmado) {
            logger.info("✅ PAGAMENTO DE RESERVA: Agendamento confirmado com sucesso! (ID Asaas: {})", paymentId);
        }

        return confirmado;
    }
}
