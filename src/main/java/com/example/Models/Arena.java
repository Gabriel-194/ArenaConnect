package com.example.Models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;

@Entity
@Table(name = "arenas", schema = "public")
@Getter
@Setter
public class Arena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "O CNPJ é obrigatório")
    @CNPJ(message = "CNPJ inválido (verifique os dígitos verificadores)")
    private String cnpj;
    private String cep;
    private String endereco;
    private String cidade;
    private String estado;

    @Column(name = "schema_name", unique = true)
    private String schemaName;

    private boolean ativo = true;

}
