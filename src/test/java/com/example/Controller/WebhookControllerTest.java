package com.example.Controller;

import com.example.Repository.ArenaRepository;
import com.example.Service.AgendamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private ArenaRepository arenaRepository;

    @Mock
    private AgendamentoService agendamentoService;

    @InjectMocks
    private WebhookController controller;

    @Test
    void rejeitaWebhookSemTokenAsaasConfigurado() {
        ReflectionTestUtils.setField(controller, "asaasWebhookAuthToken", "token-seguro-asaas-123456789012345");

        ResponseEntity<String> response = controller.handleAsaasWebhook(payloadRecebido(), "token-incorreto");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(agendamentoService, never()).confirmPaymentWebhook("pay_123");
    }

    @Test
    void processaPagamentoRecebidoQuandoTokenAsaasConfere() {
        ReflectionTestUtils.setField(controller, "asaasWebhookAuthToken", "token-seguro-asaas-123456789012345");
        when(agendamentoService.confirmPaymentWebhook("pay_123")).thenReturn(true);

        ResponseEntity<String> response = controller.handleAsaasWebhook(
                payloadRecebido(),
                "token-seguro-asaas-123456789012345"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(agendamentoService).confirmPaymentWebhook("pay_123");
    }

    private String payloadRecebido() {
        return """
                {
                  "event": "PAYMENT_RECEIVED",
                  "payment": {
                    "id": "pay_123",
                    "status": "RECEIVED"
                  }
                }
                """;
    }
}
