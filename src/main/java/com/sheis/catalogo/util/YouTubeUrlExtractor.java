package com.sheis.catalogo.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YouTubeUrlExtractor {

    private static final Pattern YOUTUBE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");

    private static final Pattern URL_PATTERNS = Pattern.compile(
            "(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|v/|shorts/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    private YouTubeUrlExtractor() {}

    /**
     * Extracts an 11-character YouTube video ID from a URL or raw ID string.
     * Returns Optional.empty() if invalid.
     */
    public static Optional<String> extractId(String input) {
        if (input == null) {
            return Optional.empty();
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (YOUTUBE_ID_PATTERN.matcher(trimmed).matches()) {
            return Optional.of(trimmed);
        }

        Matcher matcher = URL_PATTERNS.matcher(trimmed);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }

        return Optional.empty();
    }
}
