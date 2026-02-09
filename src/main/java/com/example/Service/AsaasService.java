package com.example.Service;

import com.example.DTOs.Asaas.*;
import com.example.DTOs.PartnerRegistrationDTO;
import com.example.Models.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AsaasService {

    @Value("${asaas.api.url}")
    private String asaasUrl;

    @Value("${asaas.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String createCustomer(Users user) {
        String url = asaasUrl + "/customers";

        AsaasCustumerDTO dto = new AsaasCustumerDTO();
        dto.setName(user.getNome());
        dto.setCpfCnpj(user.getCpf());
        dto.setEmail(user.getEmail());
        dto.setMobilePhone(user.getTelefone());
        dto.setExternalReference(user.getIdUser().toString());
        dto.setNotificationDisabled(true);

        HttpEntity<AsaasCustumerDTO> request = new HttpEntity<>(dto, getHeaders());

        ResponseEntity<AsaasResponseDTO> response = restTemplate.postForEntity(url, request, AsaasResponseDTO.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody().getId();
        }

        throw new RuntimeException("Falha ao criar cliente no Asaas: Resposta inesperada");
    }

    public String createWallet(PartnerRegistrationDTO dto) {
        String url = asaasUrl + "/accounts";

        HttpHeaders headers = getHeaders();

        AsaasWalletDTO walletDto = new AsaasWalletDTO();
        walletDto.setName(dto.getNomeArena());
        walletDto.setEmail(dto.getEmailAdmin());
        walletDto.setLoginEmail(dto.getEmailAdmin());
        walletDto.setCpfCnpj(dto.getCnpjArena().replaceAll("\\D", ""));
        walletDto.setCompanyType("LIMITED");
        walletDto.setIncomeValue(5000.0);
        walletDto.setMobilePhone(dto.getTelefoneUser().replaceAll("\\D", ""));

        walletDto.setAddress(dto.getEnderecoArena());
        walletDto.setAddressNumber("0");
        walletDto.setProvince(dto.getCidadeArena());
        walletDto.setPostalCode(dto.getCepArena().replaceAll("\\D", ""));

        HttpEntity<AsaasWalletDTO> request = new HttpEntity<>(walletDto, headers);

        ResponseEntity<AsaasResponseDTO> response = restTemplate.postForEntity(url, request, AsaasResponseDTO.class);

        if (response.getBody() != null) {
            if (response.getBody().getWalletId() != null) {
                return response.getBody().getWalletId();
            }
            return response.getBody().getId();
        }

        throw new RuntimeException("Erro ao criar Subconta Asaas");
    }

    public void deleteCustomer(String customerId) {
        try {
            String url = asaasUrl + "/customers/" + customerId;
            HttpEntity<String> request = new HttpEntity<>(getHeaders());
            restTemplate.delete(url);
            System.out.println("⚠️ Rollback Asaas: Cliente " + customerId + " removido.");
        } catch (Exception e) {
            System.err.println("❌ Falha ao desfazer cliente no Asaas: " + e.getMessage());
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("access_token", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "ArenaConnect-System");
        return headers;
    }

    public String createSubscription(String customerId) {
        String url = asaasUrl + "/subscriptions";

        AsaasPaymentDTO dto = new AsaasPaymentDTO();
        dto.setCustomer(customerId);
        dto.setBillingType("UNDEFINED");
        dto.setValue(100.00);
        dto.setNextDueDate(java.time.LocalDate.now().toString());
        dto.setCycle("MONTHLY");
        dto.setDescription("Assinatura ArenaConnect SaaS");

        HttpEntity<AsaasPaymentDTO> request = new HttpEntity<>(dto, getHeaders());

        ResponseEntity<AsaasResponseDTO> response = restTemplate.postForEntity(url, request, AsaasResponseDTO.class);

        if (response.getBody() != null) {
            return response.getBody().getId();
        }
        throw new RuntimeException("Erro ao criar assinatura");
    }

    public String getPaymentLink(String subscriptionId) {
        String url = asaasUrl + "/subscriptions/" + subscriptionId + "/payments";
        HttpEntity<String> request = new HttpEntity<>(getHeaders());

        ResponseEntity<Map> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, request, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("data")) {
            java.util.List<Map> data = (java.util.List<Map>) response.getBody().get("data");
            if (!data.isEmpty()) {
                return (String) data.get(0).get("invoiceUrl");
            }
        }
        return null;
    }

    public AsaasResponseDTO createPaymentWithSplit(String customerId, double valor, String walletIdArena) {
        String url =asaasUrl + "/payments";

        AsaasPaymentDTO dto = new AsaasPaymentDTO();
        dto.setCustomer(customerId);
        dto.setBillingType("UNDEFINED");
        dto.setValue(valor);
        dto.setDueDate(java.time.LocalDate.now().toString());
        dto.setDescription("Reserva de quadra - ArenaConnect");

        AsaasSplitDTO splitArena = new AsaasSplitDTO();
        splitArena.setWalletId(walletIdArena);
        splitArena.setPercentualValue(90.0);

        dto.setSplit(java.util.List.of(splitArena));

        HttpEntity<AsaasPaymentDTO> request = new HttpEntity<>(dto, getHeaders());
        ResponseEntity<AsaasResponseDTO> response = restTemplate.postForEntity(url,request, AsaasResponseDTO.class);

        if(response.getBody() != null) {
            return response.getBody();
        }

        throw new RuntimeException("Erro ao criar cobrança com split no Asaas");
    }

    public String getPaymentUrlById(String paymentId) {
        try {
            String url = asaasUrl + "/payments/" + paymentId;
            HttpEntity<String> request = new HttpEntity<>(getHeaders());

            ResponseEntity<AsaasResponseDTO> response = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    request,
                    AsaasResponseDTO.class
            );

            if (response.getBody() != null) {
                return response.getBody().getInvoiceUrl();
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar link do Asaas: " + e.getMessage());
        }
        return null;
    }

}