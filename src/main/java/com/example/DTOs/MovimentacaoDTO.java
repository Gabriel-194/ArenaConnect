package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimentacaoDTO {
    private String tempo;
    private String descricao;
    private String tipoStatus;

    public MovimentacaoDTO(String tempo, String descricao, String tipoStatus) {
        this.tempo = tempo;
        this.descricao = descricao;
        this.tipoStatus = tipoStatus;
    }
}
