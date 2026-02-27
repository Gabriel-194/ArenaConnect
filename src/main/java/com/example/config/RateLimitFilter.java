package com.example.config;

import io.github.bucket4j.Bucket;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
public class RateLimitFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

}
