package com.example.Controller;

import com.example.Models.Arena;
import com.example.Repository.ArenaRepository;
import com.example.Service.AgendamentoService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private ArenaRepository arenaRepository;

    @Value("${ASAAS_WEBHOOK_AUTH_TOKEN}")
    private String asaasWebhookAuthToken;

    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping("/asaas")
    @Transactional
    public ResponseEntity<String> handleAsaasWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "asaas-access-token", required = false) String asaasAccessToken
    ) {
        logger.info("Webhook Asaas recebido. Processando...");

        if (!isWebhookAutorizado(asaasAccessToken)) {
            logger.warn("Webhook Asaas rejeitado: token ausente ou invalido.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Webhook nao autorizado");
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            if (!root.has("event")) {
                return ResponseEntity.ok("Ignorado: payload sem evento");
            }

            String event = root.get("event").asText();
            JsonNode paymentNode = root.path("payment");
            String paymentId = paymentNode.path("id").asText();

            if ("PAYMENT_RECEIVED".equals(event) || "PAYMENT_CONFIRMED".equals(event)) {
                processarPagamentoConfirmado(paymentNode, paymentId);
            } else if ("PAYMENT_DELETED".equals(event) || "PAYMENT_REFUNDED".equals(event)) {
                agendamentoService.cancelPaymentWebhook(paymentId, event);
            } else if ("PAYMENT_OVERDUE".equals(event)
                    || "PAYMENT_SPLIT_CANCELLED".equals(event)
                    || "PAYMENT_SPLIT_DIVERGENCE_BLOCK".equals(event)
                    || "PAYMENT_SPLIT_DIVERGENCE_BLOCK_FINISHED".equals(event)) {
                logger.warn("Evento Asaas recebido para acompanhamento manual: {} - paymentId={}", event, paymentId);
            } else {
                logger.info("Evento Asaas ignorado: {}", event);
            }

            return ResponseEntity.ok("Webhook processado com sucesso");
        } catch (Exception e) {
            logger.error("Erro ao processar webhook Asaas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar webhook");
        }
    }

    private void processarPagamentoConfirmado(JsonNode paymentNode, String paymentId) {
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

    private boolean isWebhookAutorizado(String tokenRecebido) {
        if (asaasWebhookAuthToken == null || asaasWebhookAuthToken.isBlank()) {
            logger.error("asaas.webhook.auth-token nao configurado. Webhooks Asaas serao rejeitados.");
            return false;
        }

        if (tokenRecebido == null || tokenRecebido.isBlank()) {
            return false;
        }

        byte[] esperado = asaasWebhookAuthToken.trim().getBytes(StandardCharsets.UTF_8);
        byte[] recebido = tokenRecebido.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(esperado, recebido);
    }

    private void ativarArenaPorSubscription(String subscriptionId, LocalDate dataExpiracao) {
        Optional<Arena> arenaOpt = arenaRepository.findByAssasSubscriptionId(subscriptionId);

        if (arenaOpt.isPresent()) {
            Arena arena = arenaOpt.get();
            arena.setAtivo(true);
            arena.setDataExpiracao(dataExpiracao);
            arenaRepository.save(arena);

            logger.info("Arena '{}' ativada ate {}", arena.getName(), dataExpiracao);
        } else {
            logger.error("Nenhuma arena encontrada para subscription: {}", subscriptionId);
        }
    }

    public Boolean confirmPaymentifExist(String paymentId) {
        if (paymentId == null || paymentId.isEmpty()) return false;

        boolean confirmado = agendamentoService.confirmPaymentWebhook(paymentId);

        if (confirmado) {
            logger.info("Pagamento de reserva confirmado com sucesso. ID Asaas: {}", paymentId);
        }

        return confirmado;
    }
}
