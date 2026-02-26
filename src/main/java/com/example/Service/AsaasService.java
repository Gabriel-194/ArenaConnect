package com.example.Service;

import com.example.DTOs.Asaas.*;
import com.example.DTOs.FinanceiroDashboardDTO;
import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.TransacaoDTO;
import com.example.Models.Users;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );
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

    public void cancelarCobranca(String paymentId){
        if(paymentId == null || paymentId.isEmpty()) return;

        try{
            String url = asaasUrl + "/payments/" + paymentId;
            HttpEntity<String> request = new HttpEntity<>(getHeaders());

            restTemplate.exchange(url,org.springframework.http.HttpMethod.DELETE, request, String.class);
        }catch (Exception e) {
            System.err.println("Erro ao cancelar cobrança no Asaas (" + paymentId + "): " + e.getMessage());

        }
    }

    public String checkPaymentStatus(String paymentId){
        try{
            String url = asaasUrl + "/payments/" + paymentId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("access_token", apiKey);
            headers.set("User-Agent", "ArenaConnect-System");

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET,request,Map.class);

            if (response.getBody() != null) {
                return (String) response.getBody().get("status");
            }
        }catch (Exception e) {
            System.err.println("Erro ao consultar Asaas: " + e.getMessage());
        }
        return null;
    }

    @Transactional
    public FinanceiroDashboardDTO getFinanceiroDashboard() {
        FinanceiroDashboardDTO dashboard = new FinanceiroDashboardDTO();

        try {
            String balanceUrl = asaasUrl + "/finance/balance";
            ResponseEntity<Map> balanceResponse = restTemplate.exchange(
                    balanceUrl, HttpMethod.GET, new HttpEntity<>(getHeaders()), Map.class);

            if (balanceResponse.getBody() != null && balanceResponse.getBody().containsKey("balance")) {
                dashboard.setFaturamentoTotal(Double.valueOf(balanceResponse.getBody().get("balance").toString()));
            }

            String paymentUrl = asaasUrl = "/payments?limit=30";
            ResponseEntity<Map> paymentResponse = restTemplate.exchange(paymentUrl, HttpMethod.GET, new HttpEntity<>(getHeaders()), Map.class);

            List<TransacaoDTO> transacoes = new java.util.ArrayList<>();
            double aReceber = 0.0;
            double lucroSplit = 0.0;
            double lucroAssinatura = 0.0;

            if (paymentResponse.getBody() != null && paymentResponse.getBody().containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) paymentResponse.getBody().get("data");

                for (Map<String, Object> item : data) {
                    TransacaoDTO t = new TransacaoDTO();
                    t.setId((String) item.get("id"));
                    t.setData((String) item.get("dueDate"));
                    t.setStatus((String) item.get("status"));
                    t.setDescricao((String) item.get("description"));

                    Double valor = Double.valueOf(item.get("value").toString());
                    t.setValor(valor);

                    t.setCliente(item.get("customer").toString());

                    transacoes.add(t);

                    String status = t.getStatus();
                    String descricao = t.getDescricao() != null ? t.getDescricao().toLowerCase() : "";
                    if ("PENDING".equals(status) || "OVERDUE".equals(status)) {
                        aReceber += valor;
                    } else if ("RECEIVED".equals(status) || "CONFIRMED".equals(status)) {
                        // netValue é o valor limpo após as taxas do Asaas
                        Double valorLiquido = Double.valueOf(item.get("netValue").toString());

                        if (descricao.contains("assinatura")) {
                            lucroAssinatura += valorLiquido;
                        } else if (descricao.contains("reserva") || descricao.contains("quadra")) {
                            // Como os seus splits estão configurados para 90% para a arena,
                            // o netValue que cai aqui já é exatamente a sua parte (os 10% menos taxas)
                            lucroSplit += valorLiquido;
                        }
                    }
                }

            }
            dashboard.setAReceber(aReceber);
            dashboard.setLucroSplit(lucroSplit);
            dashboard.setLucroAssinatura(lucroAssinatura);
            dashboard.setTransacoes(transacoes);

        } catch (Exception e) {
            System.err.println("Erro ao gerar dashboard financeiro: " + e.getMessage());
        }

        return dashboard;
    }
}