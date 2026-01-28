package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ArenaConfigDTO {
    private LocalTime abertura;
    private LocalTime fechamento;
    private List<String> diasOperacao;
}
