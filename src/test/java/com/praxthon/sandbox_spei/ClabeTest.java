package com.praxthon.sandbox_spei;

import com.praxthon.sandbox_spei.validation.ValidadorDeReglas;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClabeTest {

    private final ValidadorDeReglas validador = new ValidadorDeReglas();

    @Test
    void vectorUno_digito9() {
        assertEquals(9, validador.digitoVerificador("03218000011835971"));
    }

    @Test
    void vectorDos_digito6() {
        assertEquals(6, validador.digitoVerificador("10315012415234578"));
    }
}