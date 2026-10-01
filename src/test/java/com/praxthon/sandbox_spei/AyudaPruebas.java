package com.praxthon.sandbox_spei;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AyudaPruebas {

    private static final Pattern ID = Pattern.compile("\"id\":\"(op_[A-Za-z0-9]+)\"");
    private static final Pattern TOTAL = Pattern.compile("\"totalElementos\":(\\d+)");

    private AyudaPruebas() {
    }

    public static String extraerId(String json) {
        Matcher m = ID.matcher(json);
        assertTrue(m.find(), "No se encontró el id en la respuesta: " + json);
        return m.group(1);
    }

    public static String idDe(ResultActions r) throws Exception {
        return extraerId(r.andReturn().getResponse().getContentAsString());
    }

    public static int total(MockMvc mvc, String ruta) throws Exception {
        String respuesta = mvc.perform(get(ruta)).andExpect(status().is(200)).andReturn().getResponse().getContentAsString();
        Matcher m = TOTAL.matcher(respuesta);
        assertTrue(m.find());
        return Integer.parseInt(m.group(1));
    }
}