package com.example.DTOs;

import java.util.List;
import java.util.Map;

public record ChatbotRequestDTO(String message, List<Map<String, String>> history) {}

