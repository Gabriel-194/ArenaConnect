package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaturamentoDTO {
    private Double valor;
    private String mes;

    public FaturamentoDTO(String mes, double valor) {
        this.mes = mes;
        this.valor = valor;
    }
}
