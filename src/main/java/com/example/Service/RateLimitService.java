package com.example.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailEnvioBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailValidacaoBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> resetSenhaBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> reservaBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> globalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> clientBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> partnerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> statusAgendamentoBuckets = new ConcurrentHashMap<>();

    public boolean RegistrarCliente(String ip) {
        return getBuckets(clientBuckets, ip, 3, Duration.ofHours(1)).tryConsume(1);
    }

    public boolean RegistrarParceiro(String ip) {
        return getBuckets(partnerBuckets, ip, 3, Duration.ofHours(1)).tryConsume(1);
    }

    public boolean limiteGlobal(String ip){
        return getBuckets(globalBuckets, ip, 60, Duration.ofMinutes(1)).tryConsume(1);
    }

    public boolean Login(String ip) {
        return getBuckets(loginBuckets, ip, 5, Duration.ofMinutes(1)).tryConsume(1);
    }

    public boolean EnviarEmail(String ip) {
        return getBuckets(emailEnvioBuckets, ip, 3, Duration.ofMinutes(10)).tryConsume(1);
    }

    public boolean Reserva(String ip) {
        return getBuckets(reservaBuckets, ip, 10, Duration.ofMinutes(1)).tryConsume(1);
    }

    public boolean ValidarCodigo(String email) {
        return getBuckets(emailValidacaoBuckets, email, 10, Duration.ofMinutes(30)).tryConsume(1);
    }

    public boolean ResetSenha(String email) {
        return getBuckets(resetSenhaBuckets, email, 5, Duration.ofMinutes(30)).tryConsume(1);
    }

    public void resetarValidacaoCodigo(String email) {
        emailValidacaoBuckets.remove(email);
    }

    public void resetarResetSenha(String email) {
        resetSenhaBuckets.remove(email);
    }

    public boolean AtualizarStatus(String ip) {
        return getBuckets(statusAgendamentoBuckets, ip, 5, Duration.ofMinutes(1)).tryConsume(1);
    }

    private Bucket getBuckets(Map<String, Bucket> mapa, String chave, int maxTokens, Duration periodo) {
        return mapa.computeIfAbsent(chave, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(maxTokens, Refill.greedy(maxTokens, periodo)))
                        .build()
        );
    }
}
