package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransacaoDTO {
    private String id;
    private String data;
    private String cliente;
    private String descricao;
    private Double valor;
    private String status;
}