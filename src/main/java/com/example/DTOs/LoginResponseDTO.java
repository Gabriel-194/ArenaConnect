package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDTO {
    private Boolean success;
    private String message;
    private String token;
    private String username;
    private String email;

    public LoginResponseDTO(){}

    public LoginResponseDTO(boolean success, String message, String token, String username, String email) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.username = username;
        this.email = email;
    }
}
