package com.example.DTOs.Asaas;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AsaasCustumerDTO {
    private String name;
    private String cpfCnpj;
    private String email;
    private String phone;
    private String mobilePhone;
    private String postalCode;
    private String address;
    private String addressNumber;
    private String complement;
    private String province;
    private String externalReference;
    private Boolean notificationDisabled = true;
}
