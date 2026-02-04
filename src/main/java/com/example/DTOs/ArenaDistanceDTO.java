package com.example.DTOs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArenaDistanceDTO {

    private Long id;
    private String name;
    private String endereco;
    private String cidade;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;


}
