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

    private String cliente_avulso;

    @Transient
    private String nomeCliente;

    @Transient
    private String quadraNome;

}
