package com.example.Service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.InvalidKeyException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:24030203194-arena-connect-2025-kuchma-2006}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    public String generateToken(Integer userId, String email, String role , Integer arenaId, String arenaSchema){
        try {
            Map<String, Object> claims = new HashMap<String, Object>();
            claims.put("userId", userId);
            claims.put("email", email);
            claims.put("role", role);

            if (arenaId != null) {
                claims.put("idArena", arenaId);
            }

            if (arenaSchema != null) {
                claims.put("arenaSchema", arenaSchema);
            } else {
                claims.put("arenaSchema", "public");
            }

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            String token = Jwts.builder().setClaims(claims).setSubject(email).setIssuedAt(now).setExpiration(expiryDate).signWith(key, SignatureAlgorithm.HS256).compact();

            logger.info("🔑 Token gerado para usuário: {} (ID: {})", email, userId);
            logger.info("⏰ Token expira em: {}", expiryDate);

            return token;
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean validateToken(String token){
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (ExpiredJwtException e) {
            logger.error("❌ Token expirado: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            logger.error("❌ Token malformado: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            logger.error("❌ Assinatura inválida: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("❌ Erro ao validar token: {}", e.getMessage());
            return false;
        }
    }

    public String getArenaSchemaFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("arenaSchema", String.class);

        } catch (Exception e) {
            logger.debug("Token não contém schemaArena");
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        try{
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.get("email", String.class);
        } catch (Exception e){
            return null;
        }
    }

}

