package com.sheis.catalogo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica el CORS con una configuración "sucia": barra final, espacios, saltos de
 * línea, mayúsculas y varios orígenes separados por coma.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class CorsConfigurationTest {

    private static final String ORIGIN_A = "https://a.example.com";
    private static final String ORIGIN_B = "https://b.example.com";
    private static final String ORIGIN_C = "https://c.example.com";

    @DynamicPropertySource
    static void corsProperties(DynamicPropertyRegistry registry) {
        registry.add("app.cors.allowed-origins",
                () -> " https://a.example.com/ ,\n  HTTPS://B.EXAMPLE.COM/// , https://c.example.com ,,");
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Origen permitido (config con barra final) responde 200 con ACAO exacto")
    void testOrigenPermitidoConBarraFinal() throws Exception {
        mockMvc.perform(get("/api/public/catalogo").header(HttpHeaders.ORIGIN, ORIGIN_A))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_A));
    }

    @Test
    @DisplayName("La comparación de orígenes es insensible a mayúsculas")
    void testComparacionInsensibleAMayusculas() throws Exception {
        mockMvc.perform(get("/api/public/catalogo").header(HttpHeaders.ORIGIN, ORIGIN_B))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_B));

        String enMayusculas = "HTTPS://B.EXAMPLE.COM";
        mockMvc.perform(get("/api/public/catalogo").header(HttpHeaders.ORIGIN, enMayusculas))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, enMayusculas));
    }

    @Test
    @DisplayName("Un origen no permitido responde 403 y no emite ACAO")
    void testOrigenNoPermitidoRetorna403SinAcao() throws Exception {
        mockMvc.perform(get("/api/public/catalogo").header(HttpHeaders.ORIGIN, "https://evil.example.com"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Preflight OPTIONS /api/public/catalogo responde 200 con ACAO y método GET")
    void testPreflightCatalogo() throws Exception {
        mockMvc.perform(options("/api/public/catalogo")
                        .header(HttpHeaders.ORIGIN, ORIGIN_A)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_A))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")));
    }

    @Test
    @DisplayName("Preflight OPTIONS /api/auth/login responde 200 con ACAO, método POST y cabeceras")
    void testPreflightLogin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, ORIGIN_C)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGIN_C))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    @DisplayName("Un origen rechazado registra WARN con el origen recibido y la lista permitida")
    void testOrigenRechazadoRegistraWarn(CapturedOutput output) throws Exception {
        String origenMalo = "https://evil.example.com";

        mockMvc.perform(get("/api/public/catalogo").header(HttpHeaders.ORIGIN, origenMalo))
                .andExpect(status().isForbidden());

        assertTrue(output.getOut().contains("CORS: origen rechazado"), output.getOut());
        assertTrue(output.getOut().contains(origenMalo), output.getOut());
        assertTrue(output.getOut().contains(ORIGIN_A), output.getOut());
    }
}
