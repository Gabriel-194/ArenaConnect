package com.example.Models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Table(name = "quadras")
@Entity
@Getter
@Setter
public class Quadra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_quadra")
    private Integer id;

    @Column(length = 100, nullable = false)
    private String nome;

    @Column(length = 100, nullable = false)
    private String tipo_quadra;

    @Column(nullable = false)
    private Double valor_hora;

    @Column
    private Boolean ativo;
}
