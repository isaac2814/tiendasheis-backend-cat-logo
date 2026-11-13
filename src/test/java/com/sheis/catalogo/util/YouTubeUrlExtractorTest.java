package com.sheis.catalogo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubeUrlExtractorTest {

    private static final String EXPECTED_ID = "dQw4w9WgXcQ";

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "http://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s&feature=share",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ?t=20",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "dQw4w9WgXcQ"
    })
    @DisplayName("Extrae ID de 11 caracteres correctamente de diversos formatos de YouTube")
    void extraeIdCorrectamente(String input) {
        Optional<String> idOpt = YouTubeUrlExtractor.extractId(input);
        assertThat(idOpt).isPresent().contains(EXPECTED_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   ",
            "https://vimeo.com/12345678",
            "https://google.com",
            "invalid_id",
            "dQw4w9WgXc", // 10 chars
            "dQw4w9WgXcQQQ" // 13 chars
    })
    @DisplayName("Retorna Optional.empty() cuando la entrada es inválida o no es de YouTube")
    void rechazaEntradasInvalidas(String input) {
        Optional<String> idOpt = YouTubeUrlExtractor.extractId(input);
        assertThat(idOpt).isEmpty();
    }

    @Test
    @DisplayName("Retorna Optional.empty() cuando el input es null")
    void manejaNull() {
        assertThat(YouTubeUrlExtractor.extractId(null)).isEmpty();
    }
}
