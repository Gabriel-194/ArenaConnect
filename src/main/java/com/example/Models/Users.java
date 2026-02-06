package com.example.Models;

import com.example.Domain.RoleEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

@Entity
@Table(name = "users", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Integer idUser;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoleEnum role;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(unique = true)
    @CPF
    private String cpf;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "token", length = 2000)
    private String token;

    @Column(name = "id_arena")
    private Integer idArena;

    private String telefone;

    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_arena", insertable = false, updatable = false)
    private Arena arena;


    public Users(String nome, RoleEnum role, String email, String senhaHash, String cpf, Boolean ativo, String telefone, Integer idArena) {
        this.nome = nome;
        this.role = role;
        this.email = email;
        this.senhaHash = senhaHash;
        this.cpf = cpf;
        this.ativo = ativo != null ? ativo : true;
        this.telefone = telefone;
        this.idArena = idArena;
    }
}