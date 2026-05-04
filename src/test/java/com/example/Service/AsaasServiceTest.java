package com.example.Service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsaasServiceTest {

    @Test
    void validarWalletSplitArenaRejeitaWalletAusente() {
        AsaasService service = new AsaasService();

        assertThrows(IllegalArgumentException.class, () -> service.validarWalletSplitArena(" "));
    }

    @Test
    void validarWalletSplitArenaRejeitaCarteiraPrincipal() {
        AsaasService service = new AsaasService();
        ReflectionTestUtils.setField(service, "masterWalletId", "wallet_master");

        assertThrows(IllegalArgumentException.class, () -> service.validarWalletSplitArena("wallet_master"));
    }

    @Test
    void validarWalletSplitArenaAceitaWalletDaArena() {
        AsaasService service = new AsaasService();
        ReflectionTestUtils.setField(service, "masterWalletId", "wallet_master");

        assertDoesNotThrow(() -> service.validarWalletSplitArena("wallet_arena"));
    }
}
