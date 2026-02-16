package com.example.Controller;

import com.example.DTOs.PartnerRegistrationDTO;
import com.example.DTOs.UserRegistrationDTO;
import com.example.DTOs.UserResponseDTO;
import com.example.Exceptions.AsaasIntegrationException;
import com.example.Models.Users;
import com.example.Service.UserService;
import com.example.Service.ArenaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ArenaService arenaService;


    @PostMapping("/register-client")
    public ResponseEntity<?> registerCliente(@RequestBody UserRegistrationDTO dto) {
        try {
            Users user = userService.registrarCliente(dto);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cliente cadastrado com sucesso");

            logger.info("✅ Cliente registrado com sucesso: {}", user.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("❌ Erro de validação no registro: {}", e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));

        } catch (Exception e) {
            logger.error("❌ Erro inesperado no registro", e);

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "Erro interno ao processar registro"
                    ));
        }
    }


    @PostMapping("/register-partner")
    public ResponseEntity<?> registerPartner(@RequestBody PartnerRegistrationDTO dto, HttpEntity<Object> httpEntity) {
      try {
          Users user = userService.registerPartner(dto);

          Map<String, Object> response = new HashMap<>();
          response.put("success", true);
          response.put("message", "admin cadastrado com sucesso");

          logger.info("✅ Cliente registrado com sucesso: {}", user.getEmail());

          return ResponseEntity.ok(response);
      } catch (IllegalArgumentException e) {

          logger.warn("❌ Erro de validação no registro: {}", e.getMessage());

          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(Map.of(
                          "success", false,
                          "message", e.getMessage()
                  ));

      } catch (AsaasIntegrationException e) {

          logger.warn("❌ Erro de integração com pagamento: {}", e.getMessage());

          return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                  .body(Map.of(
                          "success", false,
                          "message", e.getMessage()
                  ));

      } catch (Exception e) {

          logger.error("❌ Erro inesperado no registro", e);

          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                  .body(Map.of(
                          "success", false,
                          "message", "Erro interno ao processar registro"
                  ));
      }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        List<UserResponseDTO> users = userService.findAll();

        return ResponseEntity.ok(users);
    }

}