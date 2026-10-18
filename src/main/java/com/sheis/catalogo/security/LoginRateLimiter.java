package com.sheis.catalogo.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de intentos de login en memoria (válido mientras haya UNA sola instancia).
 *
 * <p>Se cuentan los intentos fallidos por IP y por correo de forma independiente:
 * 5 fallos por minuto desde la misma IP, o 5 fallos por minuto contra el mismo correo,
 * bloquean el login. Al superar el límite se responde 429 con un mensaje genérico que
 * no revela si el correo existe o no.</p>
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_SEGUNDOS = 60; // 1 minuto

    private final Map<String, List<Instant>> intentosPorIp = new ConcurrentHashMap<>();
    private final Map<String, List<Instant>> intentosPorEmail = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String ip, String email) {
        return estaBloqueada(intentosPorIp, normalizarIp(ip))
                || estaBloqueada(intentosPorEmail, normalizarEmail(email));
    }

    public void registrarFallo(String ip, String email) {
        registrar(intentosPorIp, normalizarIp(ip));
        registrar(intentosPorEmail, normalizarEmail(email));
    }

    public void registrarExito(String ip, String email) {
        intentosPorIp.remove(normalizarIp(ip));
        intentosPorEmail.remove(normalizarEmail(email));
    }

    private boolean estaBloqueada(Map<String, List<Instant>> mapa, String clave) {
        List<Instant> timestamps = mapa.get(clave);
        if (timestamps == null) {
            return false;
        }
        Instant limite = Instant.now().minusSeconds(VENTANA_SEGUNDOS);
        synchronized (timestamps) {
            limpiarAntiguos(timestamps, limite);
            return timestamps.size() >= MAX_INTENTOS;
        }
    }

    private void registrar(Map<String, List<Instant>> mapa, String clave) {
        List<Instant> timestamps = mapa.computeIfAbsent(clave, k -> new ArrayList<>());
        Instant ahora = Instant.now();
        Instant limite = ahora.minusSeconds(VENTANA_SEGUNDOS);
        synchronized (timestamps) {
            limpiarAntiguos(timestamps, limite);
            timestamps.add(ahora);
        }
    }

    private void limpiarAntiguos(List<Instant> timestamps, Instant limite) {
        Iterator<Instant> it = timestamps.iterator();
        while (it.hasNext()) {
            if (it.next().isBefore(limite)) {
                it.remove();
            }
        }
    }

    private String normalizarIp(String ip) {
        return ip != null ? ip.trim() : "unknown";
    }

    private String normalizarEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }
}
