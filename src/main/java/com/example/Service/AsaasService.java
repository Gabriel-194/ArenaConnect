package com.example.Service;

import com.example.DTOs.Asaas.*;
import com.example.DTOs.FinanceiroDashboardDTO;
import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.TransacaoDTO;
import com.example.Models.Users;
import com.example.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserRepository userRepository;

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

    public FinanceiroDashboardDTO getFinanceiroDashboard() {
        FinanceiroDashboardDTO dashboard = new FinanceiroDashboardDTO();

        try {
            dashboard.setFaturamentoTotal(buscarSaldoAtual());

            processarTransacoes(dashboard);

        } catch (Exception e) {
            System.err.println("Erro ao gerar dashboard financeiro: " + e.getMessage());
        }

        return dashboard;
    }

    private Double buscarSaldoAtual() {
        String balanceUrl = asaasUrl + "/finance/balance";
        ResponseEntity<Map> balanceResponse = restTemplate.exchange(
                balanceUrl, HttpMethod.GET, new HttpEntity<>(getHeaders()), Map.class);

        if (balanceResponse.getBody() != null && balanceResponse.getBody().containsKey("balance")) {
            return Double.valueOf(balanceResponse.getBody().get("balance").toString());
        }
        return 0.0;
    }

    private void processarTransacoes(FinanceiroDashboardDTO dashboard) {
        dashboard.setTransacoes(new java.util.ArrayList<>());
        dashboard.setAReceber(0.0);
        dashboard.setLucroSplit(0.0);
        dashboard.setLucroAssinatura(0.0);

        int offset = 0;
        int limit = 100;
        boolean hasMore = true;

        while (hasMore) {
            Map<String, Object> body = buscarPaginaAsaas(limit, offset);

            if (body != null && body.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");

                for (Map<String, Object> item : data) {
                    processarItem(item, dashboard);
                }

                Boolean asaasHasMore = (Boolean) body.get("hasMore");
                hasMore = asaasHasMore != null ? asaasHasMore : false;
                offset += limit;
            } else {
                hasMore = false;
            }
        }
    }

    private Map<String, Object> buscarPaginaAsaas(int limit, int offset) {
        String paymentsUrl = asaasUrl + "/payments?limit=" + limit + "&offset=" + offset;
        ResponseEntity<Map> response = restTemplate.exchange(
                paymentsUrl, HttpMethod.GET, new HttpEntity<>(getHeaders()), Map.class);
        return response.getBody();
    }

    private void processarItem(Map<String, Object> item, FinanceiroDashboardDTO dashboard) {
        String descricaoOriginal = (String) item.get("description");
        String descricao = descricaoOriginal != null ? descricaoOriginal.toLowerCase() : "";
        String status = (String) item.get("status");

        Double valorTotalBruto = Double.valueOf(item.get("value").toString());
        Double valorLiquido = item.get("netValue") != null && Double.valueOf(item.get("netValue").toString()) > 0
                ? Double.valueOf(item.get("netValue").toString())
                : valorTotalBruto;

        boolean isReserva = descricao.contains("reserva") || descricao.contains("quadra");
        double valorSuaFatia = isReserva ? (valorLiquido * 0.10) : valorLiquido;

        if (dashboard.getTransacoes().size() < 30) {
            dashboard.getTransacoes().add(montarTransacaoDTO(item, descricaoOriginal, status, valorSuaFatia));
        }
        atualizarTotais(dashboard, status, isReserva, valorSuaFatia);
    }

    private TransacaoDTO montarTransacaoDTO(Map<String, Object> item, String descricaoOriginal, String status, double valorSuaFatia) {
        TransacaoDTO t = new TransacaoDTO();
        t.setId((String) item.get("id"));
        t.setData((String) item.get("dueDate"));
        t.setStatus(status);
        t.setDescricao(descricaoOriginal);
        t.setValor(valorSuaFatia);

        String customerIdAsaas = item.get("customer") != null ? item.get("customer").toString() : null;
        t.setCliente(buscarDadosCliente(customerIdAsaas)); // Chama aquele método do banco de dados!

        return t;
    }

    private void atualizarTotais(FinanceiroDashboardDTO dashboard, String status, boolean isReserva, double valorSuaFatia) {
        if ("PENDING".equals(status)) {
            dashboard.setAReceber(dashboard.getAReceber() + valorSuaFatia);
        } else if ("RECEIVED".equals(status) || "CONFIRMED".equals(status)) {
            if (isReserva) {
                dashboard.setLucroSplit(dashboard.getLucroSplit() + valorSuaFatia);
            } else {
                dashboard.setLucroAssinatura(dashboard.getLucroAssinatura() + valorSuaFatia);
            }
        }
    }

    private String buscarDadosCliente(String customerIdAsaas) {
        if (customerIdAsaas == null) return "Desconhecido";

        return userRepository.findByasaasCustomerId(customerIdAsaas)
                .map(user -> {
                    String nome = user.getNome();
                    String telefone = user.getTelefone() != null ? user.getTelefone() : "Sem telefone";
                    return nome + " - " + telefone;
                })
                .orElse(customerIdAsaas);
    }
}