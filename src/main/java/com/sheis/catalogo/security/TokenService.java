package com.sheis.catalogo.security;

import com.sheis.catalogo.config.AppProperties;
import com.sheis.catalogo.web.dto.auth.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties appProperties;

    public TokenService(JwtEncoder jwtEncoder, AppProperties appProperties) {
        this.jwtEncoder = jwtEncoder;
        this.appProperties = appProperties;
    }

    public LoginResponse generarToken(String email) {
        Instant ahora = Instant.now();
        long ttlSegundos = Duration.ofHours(appProperties.getJwt().getTtlHours()).toSeconds();
        Instant expira = ahora.plusSeconds(ttlSegundos);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("tiendasheis-catalogo")
                .issuedAt(ahora)
                .expiresAt(expira)
                .subject(email)
                .claim("roles", List.of("ADMIN"))
                .build();

        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, "Bearer", ttlSegundos);
    }
}
