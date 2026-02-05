package com.example.Service;

import com.example.DTOs.Asaas.AsaasCustumerDTO;
import com.example.DTOs.Asaas.AsaasPaymentDTO;
import com.example.DTOs.Asaas.AsaasResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Service
public class AsaasService {
    private static final Logger logger = LoggerFactory.getLogger(AsaasService.class.getName());

    @Value("${asaas.api.url}")
    private String asaasApiUrl;

    @Value("${asaas.api.key}")
    private String asaasApiKey;

    @Value("${asaas.master.wallet.id}")
    private String masterWalletId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String criarCliente(AsaasCustumerDTO custumerDto) {
        try{
            String url = asaasApiUrl + "/custumers";

            HttpHeaders headers = createHeaders();
            HttpEntity<AsaasCustumerDTO> request = new HttpEntity<>(custumerDto, headers);

            ResponseEntity<AsaasResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AsaasResponseDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String customerId = response.getBody().getId();
                logger.info("✅ Cliente criado no Asaas: {}", customerId);
                return customerId;
            }

            throw new RuntimeException("Falha ao criar cliente no Asaas");

        }catch (Exception e) {
            logger.error("❌ Erro ao criar cliente no Asaas: {}", e.getMessage());
            throw new RuntimeException("Erro na integração com Asaas: " + e.getMessage());
        }
    }

    public String criarSubconta(String nomeArena, String cnpj, String email) {
        try {
            String url = asaasApiUrl + "/accounts";

            String jsonBody = String.format("""
                {
                    "name": "%s",
                    "email": "%s",
                    "cpfCnpj": "%s",
                    "companyType": "MEI",
                    "phone": "4734421234",
                    "mobilePhone": "47998765432",
                    "address": "Av. Paulista",
                    "addressNumber": "1000",
                    "province": "Centro",
                    "postalCode": "01310100"
                }
                """, nomeArena, email, cnpj);

            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<AsaasResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AsaasResponseDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String walletId = response.getBody().getId();
                logger.info("✅ Subconta criada: {}", walletId);
                return walletId;
            }

            throw new RuntimeException("Falha ao criar subconta");

        } catch (Exception e) {
            logger.error("❌ Erro ao criar subconta: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar subconta: " + e.getMessage());
        }
    }

    public AsaasResponseDTO criarCobrancaComSplit(AsaasPaymentDTO paymentDTO) {
        try {
            String url = asaasApiUrl + "/payments";

            HttpHeaders headers = createHeaders();
            HttpEntity<AsaasPaymentDTO> request = new HttpEntity<>(paymentDTO, headers);

            ResponseEntity<AsaasResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AsaasResponseDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                logger.info("✅ Cobrança criada: {}", response.getBody().getId());
                return response.getBody();
            }

            throw new RuntimeException("Falha ao criar cobrança");
        }catch (Exception e) {
            logger.error("❌ Erro ao criar cobrança: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar cobrança: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("acces_token", asaasApiKey);
        return headers;
    }

    public AsaasResponseDTO buscarPagamento(String paymentId) {
        try {
            String url = asaasApiUrl + "/payments/" + paymentId;

            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<AsaasResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    AsaasResponseDTO.class
            );

            return response.getBody();
        }catch (Exception e) {
            logger.error("❌ Erro ao buscar pagamento: {}", e.getMessage());
            return null;
        }
    }
}