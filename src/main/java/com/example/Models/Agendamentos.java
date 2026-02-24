package com.example.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "agendamentos")
public class Agendamentos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_agendamento;

    private Integer id_quadra;

    private Integer id_user;

    private LocalDateTime data_inicio;
    private LocalDateTime data_fim;

    private String status;

    @Column(name = "valor_total")
    private Double valor;


    @Transient
    private String nomeCliente;

    @Transient
    private String numeroCliente;

    @Transient
    private String quadraNome;

    @Transient
    private String schemaName;

    @Transient
    private String arenaName;

    @Transient
    private String enderecoArena;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    @Column(name = "asaas_invoice_url")
    private String asaasInvoiceUrl;

    @Column(name = "pix_qr_code")
    private String pixQrCode;

    @Column(name = "pix_copy_paste")
    private String pixCopyPaste;

}
