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

@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private UserRepository userRepository;

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

                String asaasCustomerId = root.path("payment").path("customer").asText();
                String paymentId = root.path("payment").path("id").asText();

                boolean isAgendamento = confirmPaymentifExist(paymentId);

                if(!isAgendamento){
                    if (asaasCustomerId != null && !asaasCustomerId.isEmpty()) {
                        ativarArenaPeloIdAsaas(asaasCustomerId);
                    }
                }

                logger.info("💰 Pagamento identificado. Cliente Asaas: {}", asaasCustomerId);

            }
            return ResponseEntity.ok("Webhook processado com sucesso");

        } catch (Exception e) {
            logger.error("❌ Erro ao processar webhook", e);
            return ResponseEntity.ok("Erro capturado");
        }
    }

    private void ativarArenaPeloIdAsaas(String asaasCustomerId) {
        userRepository.findByAsaasCustomerId(asaasCustomerId).ifPresentOrElse(user -> {

            if (user.getArena() != null) {
                Arena arena = user.getArena();

                if (!arena.isAtivo()) {
                    arena.setAtivo(true);
                    arenaRepository.save(arena);
                    logger.info("✅ SUCESSO! A Arena '{}' (ID: {}) foi desbloqueada!", arena.getName(), arena.getId());
                } else {
                    logger.info("ℹ️ A Arena '{}' já estava ativa. Nenhuma alteração feita.", arena.getName());
                }
            } else {
                logger.warn("⚠️ Usuário encontrado (ID: {}), mas não possui Arena vinculada.", user.getIdUser());
            }
        }, () -> {
            logger.error("❌ ERRO CRÍTICO: Nenhum usuário encontrado com o asaas_customer_id: {}", asaasCustomerId);
        });
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
