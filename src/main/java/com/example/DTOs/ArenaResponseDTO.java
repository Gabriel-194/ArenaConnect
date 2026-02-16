package com.example.DTOs;

public record ArenaResponseDTO(
        Integer idArena,
        String nome,
        String cnpj,
        String cep,
        String endereco,
        String cidade,
        String estado,
        Boolean ativo,
        String adminNome,
        String adminEmail
) {}