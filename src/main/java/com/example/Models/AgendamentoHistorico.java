package com.example.Models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos_historico", schema = "public")
@Getter
@Setter
public class AgendamentoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("id_user")
    @Column(name = "id_user")
    private Integer idUser;

    @JsonProperty("id_agendamento")
    @Column(name = "id_agendamento")
    private Integer idAgendamento;

    @Column(name = "id_quadra")
    private Integer id_quadra;

    @JsonProperty("data_inicio")
    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime data_fim;

    private String status;

    @Column(name = "valor_total")
    private Double valor;

    private Integer id_arena;

    private String arenaName;

    private String quadraNome;

    private String enderecoArena;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    @Column(name = "asaas_invoice_url")
    private String asaasInvoiceUrl;
}