package com.example.Models;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "contratos_mensalistas")
public class ContratoMensalista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "id_user")
    private Integer idUser;

    @Column(name = "id_quadra")
    private Integer idQuadra;

    @Column(name = "dia_semana")
    private Integer diaSemana; // 1 = Segunda, 2 = Terça ... 7 = Domingo

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    @Column(name = "valor_pactuado")
    private Double valorPactuado;

    private String status; // PENDENTE, PAGO, CANCELADO

    private Boolean ativo;

    @Column(name = "asaas_payment_id")
    private String asaasPaymentId;

    @Column(name = "asaas_invoice_url")
    private String asaasInvoiceUrl;

    // Construtor vazio obrigatório do JPA
    public ContratoMensalista() {}

    // --- GETTERS E SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getIdUser() { return idUser; }
    public void setIdUser(Integer idUser) { this.idUser = idUser; }

    public Integer getIdQuadra() { return idQuadra; }
    public void setIdQuadra(Integer idQuadra) { this.idQuadra = idQuadra; }

    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    public Double getValorPactuado() { return valorPactuado; }
    public void setValorPactuado(Double valorPactuado) { this.valorPactuado = valorPactuado; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public String getAsaasPaymentId() { return asaasPaymentId; }
    public void setAsaasPaymentId(String asaasPaymentId) { this.asaasPaymentId = asaasPaymentId; }

    public String getAsaasInvoiceUrl() { return asaasInvoiceUrl; }
    public void setAsaasInvoiceUrl(String asaasInvoiceUrl) { this.asaasInvoiceUrl = asaasInvoiceUrl; }
}