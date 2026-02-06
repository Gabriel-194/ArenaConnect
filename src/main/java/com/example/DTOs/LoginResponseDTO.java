package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDTO {
    private Boolean success;
    private String message;
    private String username;
    private String email;
    private Boolean arenaAtiva;
    private String paymentUrl;


    public LoginResponseDTO(boolean success, String message, String username, String email, Boolean arenaAtiva, String paymentUrl) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.email = email;
        this.arenaAtiva = arenaAtiva;
        this.paymentUrl = paymentUrl;
    }
}
