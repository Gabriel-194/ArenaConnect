package com.example.DTOs.Asaas;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsaasSplitDTO {
    private String walletId;
    private Double percentualValue; // Escolha um dos dois
    private String description;
}
