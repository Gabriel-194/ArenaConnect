package com.example.Service;

import com.example.DTOs.LoginRequestDTO;
import com.example.DTOs.LoginResponseDTO;
import com.example.Models.Users;
import com.example.Repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

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

    public LoginResponseDTO login(@RequestBody LoginRequestDTO dto) {
        logger.info("🔐 Tentativa de login: {}", dto.getEmail());

        Optional<Users> userOpt = userRepository.findByEmail(dto.getEmail());

        if (userOpt.isEmpty()) {
            return new LoginResponseDTO(false, "Email ou senha incorretos", null, null,null);
        }

        Users user = userOpt.get();

        if (!user.getAtivo()) {
            logger.warn("❌ Usuário inativo: {}", dto.getEmail());
            return new LoginResponseDTO(false, "Usuário desativado", null, null,null);
        }

        if (!passwordEncoder.matches(dto.getSenha(), user.getSenhaHash())) {
            return new LoginResponseDTO(false, "Email ou senha incorretos", null, null,null);
        }

        Integer arenaId = (user.getArena() != null) ? user.getArena().getId() : null;
        String arenaSchema = (user.getArena() != null) ? user.getArena().getSchemaName() : null;

        String token = jwtService.generateToken(
                user.getIdUser(),
                user.getEmail(),
                user.getRole().name(),
                arenaId,
                arenaSchema
        );

        user.setToken(token);
        userRepository.save(user);

        return new LoginResponseDTO(true, "Login realizado com sucesso", token, user.getNome() , user.getEmail());
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
        if(!token.equals(user.getToken())) {
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
}
