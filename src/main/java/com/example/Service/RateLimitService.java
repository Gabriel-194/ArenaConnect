package com.example.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔧 Otimização: Adicionado sistema de limpeza automática (eviction) para evitar memory leak.
 * Antes: Os mapas de buckets nunca eram limpos — IPs antigos permaneciam em memória indefinidamente.
 * Agora: Um scheduled job limpa entradas inativas a cada 30 minutos.
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    // Wrapper para rastrear último uso de cada bucket
    private static class TimedBucket {
        final Bucket bucket;
        volatile long lastAccessMillis;

        TimedBucket(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessMillis = System.currentTimeMillis();
        }

        Bucket touch() {
            this.lastAccessMillis = System.currentTimeMillis();
            return bucket;
        }
    }

    // TTL padrão: buckets não acessados por 1 hora são removidos
    private static final long BUCKET_TTL_MILLIS = Duration.ofHours(1).toMillis();

    private final Map<String, TimedBucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> emailEnvioBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> emailValidacaoBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> resetSenhaBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> reservaBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> globalBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> clientBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> partnerBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> statusAgendamentoBuckets = new ConcurrentHashMap<>();
    private final Map<String, TimedBucket> limiteNavegacaoBuckets = new ConcurrentHashMap<>();

    public boolean limiteNavegacao(String ip) {
        return getBuckets(limiteNavegacaoBuckets, ip, 300, Duration.ofMinutes(1)).tryConsume(1);
    }

    public boolean RegistrarCliente(String ip) {
        return getBuckets(clientBuckets, ip, 5, Duration.ofHours(1)).tryConsume(1);
    }

    public boolean RegistrarParceiro(String ip) {
        return getBuckets(partnerBuckets, ip, 5, Duration.ofHours(1)).tryConsume(1);
    }

    public boolean limiteGlobal(String ip) {
        return getBuckets(globalBuckets, ip, 60, Duration.ofMinutes(1)).tryConsume(1);
    }

    public boolean Login(String ip) {
        return getBuckets(loginBuckets, ip, 5, Duration.ofMinutes(1)).tryConsume(1);
    }

    public boolean EnviarEmail(String ip) {
        return getBuckets(emailEnvioBuckets, ip, 3, Duration.ofMinutes(10)).tryConsume(1);
    }

    public boolean Reserva(String ip) {
        return getBuckets(reservaBuckets, ip, 10, Duration.ofMinutes(5)).tryConsume(1);
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
        return getBuckets(statusAgendamentoBuckets, ip, 10, Duration.ofMinutes(5)).tryConsume(1);
    }

    private Bucket getBuckets(Map<String, TimedBucket> mapa, String chave, int maxTokens, Duration periodo) {
        return mapa.computeIfAbsent(chave, k ->
                new TimedBucket(
                        Bucket.builder()
                                .addLimit(Bandwidth.classic(maxTokens, Refill.greedy(maxTokens, periodo)))
                                .build()
                )
        ).touch();
    }

    /**
     * 🔧 Limpeza automática de buckets inativos — roda a cada 30 minutos.
     * Remove entradas que não foram acessadas no último BUCKET_TTL_MILLIS (1 hora).
     * Evita memory leak com IPs que nunca mais retornam.
     */
    @Scheduled(fixedRate = 1800000) // 30 minutos
    public void evictExpiredBuckets() {
        long now = System.currentTimeMillis();
        int totalRemovido = 0;

        totalRemovido += evict(loginBuckets, now);
        totalRemovido += evict(emailEnvioBuckets, now);
        totalRemovido += evict(emailValidacaoBuckets, now);
        totalRemovido += evict(resetSenhaBuckets, now);
        totalRemovido += evict(reservaBuckets, now);
        totalRemovido += evict(globalBuckets, now);
        totalRemovido += evict(clientBuckets, now);
        totalRemovido += evict(partnerBuckets, now);
        totalRemovido += evict(statusAgendamentoBuckets, now);
        totalRemovido += evict(limiteNavegacaoBuckets, now);

        if (totalRemovido > 0) {
            logger.info("🧹 RateLimit: {} buckets expirados removidos da memória.", totalRemovido);
        }
    }

    private int evict(Map<String, TimedBucket> mapa, long now) {
        int antes = mapa.size();
        mapa.entrySet().removeIf(e -> (now - e.getValue().lastAccessMillis) > BUCKET_TTL_MILLIS);
        return antes - mapa.size();
    }
}
