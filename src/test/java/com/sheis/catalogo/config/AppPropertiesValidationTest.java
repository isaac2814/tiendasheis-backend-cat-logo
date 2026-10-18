package com.sheis.catalogo.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppPropertiesValidationTest {

    private AppProperties conSecretos(String jwtSecret, String adminEmail, String adminPassword) {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret(jwtSecret);
        props.getAdmin().setEmail(adminEmail);
        props.getAdmin().setPassword(adminPassword);
        return props;
    }

    @Test
    @DisplayName("JWT_SECRET vacío impide el arranque")
    void testJwtVacioFalla() {
        AppProperties props = conSecretos("", "admin@test.com", "AdminPasswordTest123!");
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("JWT_SECRET corto (<32) impide el arranque")
    void testJwtCortoFalla() {
        AppProperties props = conSecretos("demasiado-corto", "admin@test.com", "AdminPasswordTest123!");
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("JWT_SECRET de ejemplo/desarrollo impide el arranque")
    void testJwtDeEjemploFalla() {
        AppProperties props = conSecretos(
                "super_secret_jwt_key_at_least_32_characters_123456",
                "admin@test.com",
                "AdminPasswordTest123!");
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("ADMIN_EMAIL/ADMIN_PASSWORD faltantes impiden el arranque")
    void testAdminFaltanteFalla() {
        String secretoValido = "secreto_de_pruebas_suficientemente_largo_0123456789";
        assertThrows(IllegalStateException.class,
                () -> conSecretos(secretoValido, "", "AdminPasswordTest123!").validate());
        assertThrows(IllegalStateException.class,
                () -> conSecretos(secretoValido, "admin@test.com", "").validate());
    }

    @Test
    @DisplayName("ADMIN_PASSWORD débil (<10) impide el arranque")
    void testAdminPasswordDebilFalla() {
        AppProperties props = conSecretos(
                "secreto_de_pruebas_suficientemente_largo_0123456789",
                "admin@test.com",
                "corta123");
        assertThrows(IllegalStateException.class, props::validate);
    }

    @Test
    @DisplayName("Configuración válida no lanza excepción")
    void testConfiguracionValidaPasa() {
        AppProperties props = conSecretos(
                "secreto_de_pruebas_suficientemente_largo_0123456789",
                "admin@test.com",
                "AdminPasswordTest123!");
        assertDoesNotThrow(props::validate);
    }
}
