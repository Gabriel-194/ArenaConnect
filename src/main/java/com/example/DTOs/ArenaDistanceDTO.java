package com.example.DTOs;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class ArenaDistanceDTO {

    private Long id;
    private String name;
    private String endereco;
    private String cidade;
    private String estado;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private String horaInicio;
    private String horaFim;

}
