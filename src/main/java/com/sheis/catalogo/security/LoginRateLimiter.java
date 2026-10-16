package com.sheis.catalogo.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_SEGUNDOS = 15 * 60; // 15 minutos

    private final Map<String, List<Instant>> intentosFallidos = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String ip, String email) {
        String key = buildKey(ip, email);
        List<Instant> timestamps = intentosFallidos.get(key);
        if (timestamps == null) {
            return false;
        }

        Instant limite = Instant.now().minusSeconds(VENTANA_SEGUNDOS);
        synchronized (timestamps) {
            limpiarAntiguos(timestamps, limite);
            return timestamps.size() >= MAX_INTENTOS;
        }
    }

    public void registrarFallo(String ip, String email) {
        String key = buildKey(ip, email);
        List<Instant> timestamps = intentosFallidos.computeIfAbsent(key, k -> new ArrayList<>());
        Instant ahora = Instant.now();
        Instant limite = ahora.minusSeconds(VENTANA_SEGUNDOS);

        synchronized (timestamps) {
            limpiarAntiguos(timestamps, limite);
            timestamps.add(ahora);
        }
    }

    public void registrarExito(String ip, String email) {
        String key = buildKey(ip, email);
        intentosFallidos.remove(key);
    }

    private void limpiarAntiguos(List<Instant> timestamps, Instant limite) {
        Iterator<Instant> it = timestamps.iterator();
        while (it.hasNext()) {
            if (it.next().isBefore(limite)) {
                it.remove();
            }
        }
    }

    private String buildKey(String ip, String email) {
        String safeIp = ip != null ? ip.trim() : "unknown";
        String safeEmail = email != null ? email.trim().toLowerCase() : "";
        return safeIp + ":" + safeEmail;
    }
}
