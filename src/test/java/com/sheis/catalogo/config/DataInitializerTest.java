package com.sheis.catalogo.config;

import com.sheis.catalogo.domain.AdminUsuario;
import com.sheis.catalogo.repository.AdminUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    private static final String EMAIL = "admin@test.com";
    private static final String PASSWORD_INICIAL = "PasswordInicial123!";
    private static final String PASSWORD_NUEVA = "PasswordNueva123!";

    @Mock
    private AdminUsuarioRepository adminUsuarioRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AppProperties appProperties;
    private DataInitializer dataInitializer;
    private AdminUsuario adminExistente;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getAdmin().setEmail(EMAIL);
        appProperties.getAdmin().setPassword(PASSWORD_NUEVA);
        dataInitializer = new DataInitializer(adminUsuarioRepository, appProperties, passwordEncoder);
        adminExistente = AdminUsuario.builder()
                .id(1L)
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(PASSWORD_INICIAL))
                .nombre("Administrador")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("ADMIN_RESET_PASSWORD=true actualiza el hash del admin existente")
    void testResetActualizaPasswordExistente() {
        appProperties.getAdmin().setResetPassword("true");
        when(adminUsuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(adminExistente));

        dataInitializer.run(null);

        assertTrue(passwordEncoder.matches(PASSWORD_NUEVA, adminExistente.getPasswordHash()));
        assertFalse(passwordEncoder.matches(PASSWORD_INICIAL, adminExistente.getPasswordHash()));
        verify(adminUsuarioRepository).save(adminExistente);
        verify(adminUsuarioRepository, never()).count();
    }

    @Test
    @DisplayName("ADMIN_RESET_PASSWORD=true crea el admin si el correo no existe")
    void testResetCreaAdminSiNoExiste() {
        appProperties.getAdmin().setResetPassword("true");
        appProperties.getAdmin().setEmail("  Nuevo.Admin@Test.com ");
        when(adminUsuarioRepository.findByEmailIgnoreCase("nuevo.admin@test.com"))
                .thenReturn(Optional.empty());

        dataInitializer.run(null);

        ArgumentCaptor<AdminUsuario> captor = ArgumentCaptor.forClass(AdminUsuario.class);
        verify(adminUsuarioRepository).save(captor.capture());
        AdminUsuario creado = captor.getValue();
        assertEquals("nuevo.admin@test.com", creado.getEmail());
        assertTrue(passwordEncoder.matches(PASSWORD_NUEVA, creado.getPasswordHash()));
        assertTrue(creado.getActivo());
        assertNotNull(creado.getCreadoEn());
        verify(adminUsuarioRepository, never()).count();
    }

    @Test
    @DisplayName("Sin ADMIN_RESET_PASSWORD=true no se toca la contraseña existente")
    void testSinResetNoModificaPassword() {
        appProperties.getAdmin().setResetPassword("false");
        when(adminUsuarioRepository.count()).thenReturn(1L);

        dataInitializer.run(null);

        assertTrue(passwordEncoder.matches(PASSWORD_INICIAL, adminExistente.getPasswordHash()));
        verify(adminUsuarioRepository, never()).save(any(AdminUsuario.class));
        verify(adminUsuarioRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Sin reset se mantiene la creación del admin inicial si la tabla está vacía")
    void testSinResetMantieneCreacionInicial() {
        when(adminUsuarioRepository.count()).thenReturn(0L);

        dataInitializer.run(null);

        ArgumentCaptor<AdminUsuario> captor = ArgumentCaptor.forClass(AdminUsuario.class);
        verify(adminUsuarioRepository).save(captor.capture());
        AdminUsuario creado = captor.getValue();
        assertEquals(EMAIL, creado.getEmail());
        assertTrue(passwordEncoder.matches(PASSWORD_NUEVA, creado.getPasswordHash()));
    }
}
