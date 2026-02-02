package com.example.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos_historico", schema = "public") // Fica sempre no public
@Getter
@Setter
public class AgendamentoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_user", nullable = false)
    private Integer idUser; // Seu sistema usa Integer para user

    @Column(name = "schema_name", nullable = false)
    private String schemaName;

    @Column(name = "id_origem", nullable = false)
    private Integer idAgendamentoArena; // ID do agendamento dentro da arena

    @Column(name = "nome_arena")
    private String nomeArena;

    @Column(name = "nome_quadra")
    private String nomeQuadra;

    @Column(name = "endereco_resumido")
    private String enderecoResumido;

    @Column(name = "data_inicio")
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    private String status;

    private Double valor;
}