package com.sheis.catalogo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OutputCaptureExtension.class)
class CorsOriginsNormalizationTest {

    private AppProperties props(String cors) {
        AppProperties props = new AppProperties();
        props.getCors().setAllowedOrigins(cors);
        return props;
    }

    @Test
    @DisplayName("Quita barra final, espacios y saltos de línea de un único origen")
    void testNormalizaBarraEspaciosYSaltosDeLinea() {
        AppProperties props = props("  https://A.example.com/  \n");

        assertEquals(List.of("https://a.example.com"), props.getCors().getAllowedOriginsList());
    }

    @Test
    @DisplayName("Normaliza varios orígenes separados por coma, ignorando vacíos")
    void testVariosOrigenesSeparadosPorComa() {
        AppProperties props = props(" https://a.example.com/ ,\n  https://B.example.com/// ,, ");

        assertEquals(
                List.of("https://a.example.com", "https://b.example.com"),
                props.getCors().getAllowedOriginsList());
    }

    @Test
    @DisplayName("Ignora entradas vacías y elimina duplicados sin distinguir mayúsculas")
    void testVaciosYDuplicados() {
        AppProperties props = props(", , https://a.example.com/ , HTTPS://A.EXAMPLE.COM ,");

        assertEquals(List.of("https://a.example.com"), props.getCors().getAllowedOriginsList());
    }

    @Test
    @DisplayName("Al arrancar registra en INFO la lista final de orígenes permitidos")
    void testLogInfoConLaListaFinal(CapturedOutput output) {
        AppProperties props = props("https://A.example.com/");
        props.getJwt().setSecret("secreto_de_pruebas_suficientemente_largo_0123456789");
        props.getAdmin().setEmail("admin@test.com");
        props.getAdmin().setPassword("AdminPasswordTest123!");

        props.validate();

        assertTrue(output.getOut().contains("CORS: orígenes permitidos"), output.getOut());
        assertTrue(output.getOut().contains("https://a.example.com"), output.getOut());
    }
}
