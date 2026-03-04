package com.example.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GroqService {
    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_CONTEXT = """
        Você é um assistente virtual do Arena Connect, um sistema SaaS para gestão de arenas esportivas.
        
        SUAS FUNÇÕES:
        1. Ajudar usuários a se cadastrarem (Cliente ou Parceiro)
        2. Explicar funcionalidades do sistema
        3. Guiar na navegação
        4. Responder dúvidas sobre agendamentos, quadras
        5. Responda de forma curta, efetiva, simples e amigavel
        
        FLUXO DE CADASTRO DE ADMIN:
                        Se o usuário quiser se cadastrar como DONO DE ARENA, Parceiro ou Admin, NÃO peça nenhum dado.
                        Responda APENAS com o seguinte texto exato:
                        [OPEN_PARTNER_MODAL]
        
        FLUXO DE CADASTRO DE CLIENTE PELO CHAT:
                        Se o usuário quiser se cadastrar, você DEVE atuar como um formulário conversacional.
                        Peça os seguintes dados UM POR UM (nunca peça todos de uma vez):
                        1. Nome completo
                        2. E-mail
                        3. CPF
                        4. TELEFONE
                        5. Senha
                        6. CONFIRMAR SENHA
                        
        REGRAS DE COLETA:
        - CPF: Aceite com ou sem pontos/traços. Não force o usuário a colocar máscara.
        - TELEFONE: Aceite apenas números ou com parênteses/espaços.
                    
        Regra de Ouro: QUANDO VOCÊ TIVER COLETADO OS 6 DADOS, não diga mais nada. Responda APENAS com o seguinte texto exato (substituindo pelos dados reais), sem blocos de código markdown:
        [REGISTER_CLIENT_CMD] {"nome": "nome do usuario", "email": "email", "cpf": "cpf", "telefone": "telefone", "senha": "senha", "confirmarSenha":"confirmarSenha"}
        
        TIPOS DE USUÁRIO:
        - Cliente: Pessoa que quer reservar quadras
        - Dono de arena: Dono de arena que quer gerenciar seu complexo
        
        FUNCIONALIDADES PRINCIPAIS:
        - Dashboard: Visão geral e relatórios
        - Agendamentos: Calendário de jogos e reservas
        - Quadras: Gerenciamento de espaços esportivos
        
        INSTRUÇÕES:
        - Seja cordial e objetivo
        - Forneça passos claros quando ensinar algo
        - Se não souber, seja honesto
        - Não invente informações técnicas
        - Não fale sobre dados sensiveis
    """;

    public String chat(String userMessage,List<Map<String, String>> conversationHistory) {
        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            messages.add(Map.of("role", "system", "content", SYSTEM_CONTEXT));

            if (conversationHistory != null) {
                int startIndex = Math.max(0, conversationHistory.size() - 10);

                List<Map<String, String>> shortHistory = conversationHistory.subList(startIndex, conversationHistory.size());

                for (Map<String, String> msg : shortHistory) {
                    messages.add(Map.of("role", msg.get("role"), "content", msg.get("text")));
                }
            }

            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> requestBody = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", messages,
                    "temperature", 0.7
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GROQ_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return "Desculpe, não consegui processar a sua mensagem. Tente novamente.";
        }catch (Exception e) {
            e.printStackTrace();
            return "Ocorreu um erro de comunicação com a IA. Por favor, tente novamente em instantes.";
        }
    }

    public String getQuickResponse(String type) {
        return switch (type) {
            case "como_cadastrar_cliente" -> """
                Ótimo! Vamos fazer o seu cadastro agora mesmo por aqui. 
                
                Para começarmos, por favor, me diga o seu Nome Completo:
                """;

            case "como_cadastrar_admin" -> """
                    Para se cadastrar como Dono de Arena:
                    
                    1. Clique em "Criar conta"
                    2. Escolha "Sou Dono de Arena"
                    3. Preencha seus dados pessoais
                    4. Preencha os dados da sua arena (Nome, CNPJ, CEP)
                    5. Finalize o cadastro
                    
                    ✅ Após isso, você terá acesso ao painel de gestão!
                    
                    Estou te redirecionando para a tela de cadastro ...
                    [OPEN_PARTNER_MODAL]
                    """;

            case "funcionalidades" -> """
                    🎯 Principais funcionalidades do Arena Connect:
                    
                    📊 Dashboard - Visão geral do seu negócio
                    📅 Agendamentos - Controle de reservas
                    🎾 Quadras - Gerencie seus espaços

                    
                    Sobre o que deseja saber mais?
                    """;

            default -> "Como posso ajudá-lo?";
        };

    }
}
