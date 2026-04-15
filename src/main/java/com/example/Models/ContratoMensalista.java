package com.example.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "contratos_mensalistas")
@Setter
@Getter
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

    @Transient
    private Integer idArena;

    @Transient
    private String arenaName;

    @Transient
    private Boolean temDesconto;

    // Construtor vazio obrigatório do JPA
    public ContratoMensalista() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContratoMensalista that = (ContratoMensalista) o;
        return Objects.equals(id, that.id) && Objects.equals(idArena, that.idArena);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idArena);
    }
}