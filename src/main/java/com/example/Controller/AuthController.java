package com.example.Controller;

import com.example.DTOs.LoginRequestDTO;
import com.example.DTOs.LoginResponseDTO;
import com.example.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequestDTO, HttpServletResponse response) {
        try{
            LoginResponseDTO LoginResponse = authService.login(loginRequestDTO, response);

            if (LoginResponse.getSuccess()){
                return ResponseEntity.ok(LoginResponse);

            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(LoginResponse);
            }
        } catch (Exception e){
            e.printStackTrace();
            LoginResponseDTO loginResponseDTO = new LoginResponseDTO(
                    false,
                    "erro interno ao processar login",
                    null,
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginResponseDTO);
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        try {
            String token = extractToken(request);

            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Token não fornecido"));
            }

            boolean isValid = authService.validateToken(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("valid", false, "message", "Erro ao validar token"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        try{
            String token = extractToken(request);

            if (token == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Token não fornecido"));
            }

            authService.logout(token);

            Cookie cookie = new Cookie("accessToken", null);
            cookie.setHttpOnly(true);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge(0);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Logout realizado com sucesso"
            ));

        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Erro ao realizar logout"));
        }
    }

    private String extractToken(HttpServletRequest request) {
       Cookie[] cookies = request.getCookies();
       if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
       }
       return null;
    }


}
