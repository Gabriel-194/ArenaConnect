package com.example.Service;

import com.example.DTOs.LoginRequestDTO;
import com.example.DTOs.LoginResponseDTO;
import com.example.Models.Arena;
import com.example.Models.Users;
import com.example.Repository.ArenaRepository;
import com.example.Repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.Domain.RoleEnum;


import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtService jwtService;
    @Autowired
    private AsaasService asaasService;

    @Autowired
    private ArenaRepository arenaRepository;

    @Value("${google.client.id}")
    private String googleClientId;

    public LoginResponseDTO login(LoginRequestDTO dto, HttpServletResponse response) {
        logger.info("🔐 Tentativa de login: {}", dto.getEmail());

        Optional<Users> userOpt = userRepository.findByEmail(dto.getEmail());

        if (userOpt.isEmpty()) {
            return new LoginResponseDTO(false, "Email ou senha incorretos", null, null, false, null);
        }

        Users user = userOpt.get();

        if (!user.getAtivo()) {
            logger.warn("❌ Usuário inativo: {}", dto.getEmail());
            return new LoginResponseDTO(false, "Usuário desativado", null, null, false, null);
        }

        if (!passwordEncoder.matches(dto.getSenha(), user.getSenhaHash())) {
            return new LoginResponseDTO(false, "Email ou senha incorretos", null, null, false, null);
        }

        return efetivarSessao(user,response);
    }

    private LoginResponseDTO efetivarSessao(Users user, HttpServletResponse response ){

        Integer arenaId = (user.getArena() != null) ? user.getArena().getId() : null;
        String arenaSchema = (user.getArena() != null) ? user.getArena().getSchemaName() : null;

        String token = jwtService.generateToken(
                user.getIdUser(),
                user.getEmail(),
                user.getRole().name(),
                arenaId,
                arenaSchema,
                user.getNome()
        );

        user.setToken(token);
        userRepository.save(user);

        jakarta.servlet.http.Cookie jwtCookie = jwtService.createJwtCookie(token);
        response.addCookie(jwtCookie);

        Map<String, Object> status = verifyArenaStatus(user);
        boolean arenaAtiva = (boolean) status.get("arenaAtiva");
        String paymentUrl = (String) status.get("paymentUrl");

        return new LoginResponseDTO(
                true,
                "Login realizado com sucesso",
                user.getNome(),
                user.getEmail(),
                arenaAtiva,
                paymentUrl
        );
    }

    public boolean validateToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        boolean isValid = jwtService.validateToken(token);
        if (!isValid) {
            return false;
        }

        String email = jwtService.getEmailFromToken(token);
        Optional<Users> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return false;
        }

        Users user = userOpt.get();
        if (!token.equals(user.getToken())) {
            return false;
        }

        logger.info("✅ Token válido para: {}", email);
        return true;
    }

    @Transactional
    public void logout(String token) {
        String email = jwtService.getEmailFromToken(token);
        Optional<Users> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            Users user = userOpt.get();
            user.setToken(null);
            userRepository.save(user);
            logger.info("✅ Logout realizado: {}", email);
        }
    }

    public Users getUserByToken(String token) {
        String email = jwtService.getEmailFromToken(token);
        if (email == null) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    public String determineRedirectUrl(RoleEnum role) {
        if (role == null) return "/";

        switch (role) {
            case ADMIN:
                return "/home";
            case SUPERADMIN:
                return "/homeSuperAdmin";
            case CLIENTE:
                return "/homeClient";

            default:
                return "/";
        }
    }

    public Map<String, Object> verifyArenaStatus(Users user) {

        Boolean arenaAtiva = true;
        String paymentUrl = null;

        if (user.getRole() == RoleEnum.ADMIN || user.getIdArena() != null) {
            Arena arena = null;

            if (user.getArena() != null) {
                arena = user.getArena();
            } else if (user.getIdArena() != null) {
                arena = arenaRepository.findById(user.getIdArena().longValue())
                        .orElse(null);
            }

            if (arena != null) {
                if(arena.getDataExpiracao() != null) {
                    if(LocalDate.now().isAfter(arena.getDataExpiracao())) {
                        arena.setAtivo(false);
                        arenaRepository.save(arena);
                        arenaAtiva = false;
                    }else {
                        arenaAtiva = true;
                    }
                } else {
                    arenaAtiva = arena.isAtivo();
                }

                if (Boolean.FALSE.equals(arenaAtiva)) {
                    String subId = arena.getAssasSubscriptionId();

                    if (subId != null) {
                        try {
                            paymentUrl = asaasService.getPaymentLink(subId);
                        } catch (Exception e) {
                            logger.error(
                                    "Erro ao buscar link Asaas para o user {}: {}",
                                    user.getEmail(),
                                    e.getMessage()
                            );
                        }
                    }
                }
            }
        }
        Map<String, Object> status = new HashMap<>();
        status.put("arenaAtiva", arenaAtiva);
        status.put("paymentUrl", paymentUrl);
        return status;
    }

    public Map<String, Object> processarLoginGoogle(String tokenString, HttpServletResponse response) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(tokenString);

        if (idToken == null) {
            throw new IllegalArgumentException("Token do Google inválido ou forjado.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        Optional<Users> userOpt = userRepository.findByEmail(email);

        Map<String, Object> resultado = new HashMap<>();

        if(userOpt.isPresent()){
            Users user = userOpt.get();

            if (!user.getAtivo()) {
                throw new RuntimeException("Usuário desativado");
            }

            LoginResponseDTO loginFeito = efetivarSessao(user,response);

            resultado.put("isNewUser", false);
            resultado.put("username", loginFeito.getUsername());
            resultado.put("arenaAtiva", loginFeito.getArenaAtiva());
            resultado.put("paymentUrl", loginFeito.getPaymentUrl());

        } else{
            resultado.put("isNewUser", true);

            Map<String, String> googleData = new HashMap<>();
            googleData.put("name", name);
            googleData.put("email", email);

            resultado.put("googleData", googleData);
        }
        return resultado;
    }
}

