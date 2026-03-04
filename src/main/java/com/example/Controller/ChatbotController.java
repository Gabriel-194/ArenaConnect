package com.example.Controller;

import aj.org.objectweb.asm.TypeReference;
import com.example.DTOs.ChatbotRequestDTO;
import com.example.DTOs.UserRegistrationDTO;
import com.example.Service.GroqService;
import com.example.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "http://localhost:5173")
public class ChatbotController {

    @Autowired
    private GroqService groqService;

     @Autowired
     private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/message")
    public ResponseEntity<Map<String,Object>> handleMessage(@RequestBody ChatbotRequestDTO request) {
        Map<String,Object> response = new HashMap<>();
        String actionCommand = null;

        try {
            String aiResponse = groqService.chat(request.message(), request.history());

            if (aiResponse.contains("[REGISTER_CLIENT_CMD]")) {
                try {
                    String jsonStr = aiResponse
                            .replace("[REGISTER_CLIENT_CMD]", "")
                            .replace("```json", "")
                            .replace("```", "")
                            .trim();

                    UserRegistrationDTO userData = objectMapper.readValue(jsonStr, UserRegistrationDTO.class);

                    String cpf = userData.getCpf().replaceAll("\\D", "");
                    String telefone = userData.getTelefone().replaceAll("\\D", "");
                    userData.setCpf( cpf);
                    userData.setTelefone(telefone);

                     userService.registrarCliente(userData);

                    aiResponse = "✅ Cadastro realizado com sucesso! Pode fechar o chat e fazer o login. Bem-vindo ao Arena Connect!";

                } catch (Exception e) {
                    System.err.println("Erro ao converter JSON do Cadastro: " + e.getMessage());
                    aiResponse = "❌ Ops, ocorreu um erro ao tentar processar o seu cadastro no sistema. Por favor, tente novamente.";
                }
            } else if (aiResponse.contains("[OPEN_PARTNER_MODAL]")) {
                actionCommand = "OPEN_PARTNER_MODAL";
                aiResponse = "Excelente! Estou a abrir o formulário seguro de parceiros para si. Preencha os dados e ajuste a localização da sua arena no mapa! 🏟️";
            }

            response.put("success", true);
            response.put("response", aiResponse);
            if (actionCommand != null) {
                response.put("action", actionCommand);
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao se comunicar com o assistente virtual.");
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/quick-response/{actionId}")
    public ResponseEntity<Map<String, Object>> handleQuickAction(@PathVariable String actionId) {
        Map<String,Object> response = new HashMap<>();
        String actionCommand = null;

        try{
            String text = groqService.getQuickResponse(actionId);

             if (text.contains("[OPEN_PARTNER_MODAL]")) {
                 actionCommand = "OPEN_PARTNER_MODAL";
                 text = text.replace("[OPEN_PARTNER_MODAL]", "").trim();
             }

            response.put("success", true);
            response.put("response", text);
            if (actionCommand != null) {
                response.put("action", actionCommand);
            }
            return ResponseEntity.ok(response);
        }catch (Exception e) {
            response.put("success", false);
            return ResponseEntity.status(500).build();
        }
    }
}
