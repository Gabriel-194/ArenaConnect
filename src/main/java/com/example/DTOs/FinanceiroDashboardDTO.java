package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class FinanceiroDashboardDTO {
    private Double faturamentoTotal = 0.0;
    private Double aReceber = 0.0;
    private Double lucroSplit = 0.0;
    private Double lucroAssinatura = 0.0;
    private List<TransacaoDTO> transacoes;
}