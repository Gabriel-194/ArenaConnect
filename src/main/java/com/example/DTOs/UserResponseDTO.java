package com.example.DTOs;

import com.example.Domain.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Integer idUser;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    private RoleEnum role;
    private Boolean ativo;
}