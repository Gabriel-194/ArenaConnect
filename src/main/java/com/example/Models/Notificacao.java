package com.example.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacoes", schema = "public")
@Getter
@Setter
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Boolean lida = false;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    public Notificacao() {}

    public Notificacao(Long userId, String titulo, String mensagem, String tipo) {
        this.userId = userId;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.tipo = tipo;
        this.lida = false;
    }


    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }
}