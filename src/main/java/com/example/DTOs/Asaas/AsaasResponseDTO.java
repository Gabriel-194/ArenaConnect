package com.example.DTOs.Asaas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AsaasResponseDTO {
    private String id;
    private String customer;
    private String invoiceUrl;
    private String bankSlipUrl;
    private PixInfoDTO pix;
    private String status;
    private String externalReference;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PixInfoDTO {
        private String encodedImage;
        private String payload;
        private String expirationDate;
    }
}
