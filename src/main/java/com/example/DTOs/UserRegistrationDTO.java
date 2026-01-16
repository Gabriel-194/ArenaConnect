package com.example.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    private String role;

    @NotBlank(message = "CPF é obrigatório")
    @CPF(message = "CPF inválido")
    private String cpf;

    private String telefone;
    private Integer idArena;

    public boolean isEmailValid() {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public String getCpfLimpo() {
        return cpf != null ? cpf.replaceAll("\\D", "") : null;

    }

    public String getTelefoneLimpo() {
        if (telefone == null) return null;
        return telefone.replaceAll("\\D", "");
    }


}