package com.example.DTOs.Asaas;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class AsaasPaymentDTO {
    private String customer;
    private String billingType;
    private Double value;
    private String dueDate;
    private String description;
    private String externalReference;
    private List<AsaasSplitDTO> split;
}
