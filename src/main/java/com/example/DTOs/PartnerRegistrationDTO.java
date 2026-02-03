package com.example.DTOs;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
public class PartnerRegistrationDTO {


    @NotBlank(message = "Nome é obrigatório")
    private String nomeUser;

    @NotBlank(message = "CPF é obrigatório")
    @CPF(message = "CPF inválido")
    private String cpfUser;

    private String telefoneUser;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String emailAdmin;

    @NotBlank(message = "Senha é obrigatória")
    private String senhaAdmin;

    private String confirmarSenha;

    @NotBlank(message = "Nome da arena é obrigatório")
    private String nomeArena;

    @NotBlank(message = "CNPJ é obrigatório")
    @CNPJ(message = "CNPJ inválido")
    private String cnpjArena;

    @NotBlank(message = "CEP é obrigatório")
    private String cepArena;

    private String enderecoArena;
    private String cidadeArena;
    private String estadoArena;

    private Double latitude;
    private Double longitude;

    public String getCpfLimpo() {
        return cpfUser != null ? cpfUser.replaceAll("\\D", "") : null;

    }
}