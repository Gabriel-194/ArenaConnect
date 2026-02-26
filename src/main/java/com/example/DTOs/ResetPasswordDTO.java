package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ResetPasswordDTO {
    private String email;
    private String token;
    private String newPassword;


}