package com.sheis.catalogo.service.media;

import com.sheis.catalogo.config.AppProperties;
import com.sheis.catalogo.exception.ServiceUnavailableException;
import com.sheis.catalogo.exception.UnprocessableEntityException;
import com.sheis.catalogo.web.dto.admin.MediaKind;
import com.sheis.catalogo.web.dto.admin.PresignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaServiceTest {

    private InMemoryMediaStorage storage;
    private AppProperties appProperties;
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        storage = new InMemoryMediaStorage("https://media.tiendasheis.com");
        appProperties = new AppProperties();
        AppProperties.R2 r2 = new AppProperties.R2();
        r2.setEnabled(true);
        r2.setBucket("sheis-media");
        r2.setPublicBaseUrl("https://media.tiendasheis.com");
        appProperties.setR2(r2);
        mediaService = new MediaService(storage, appProperties);
    }

    @Test
    @DisplayName("Lanza ServiceUnavailableException si el storage está deshabilitado")
    void fallaSiStorageDeshabilitado() {
        NoopMediaStorage noopStorage = new NoopMediaStorage();
        MediaService serviceDisabled = new MediaService(noopStorage, appProperties);

        PresignRequest req = new PresignRequest(MediaKind.PRODUCT_VIDEO, "video/mp4", 5_000_000L);

        assertThatThrownBy(() -> serviceDisabled.presign(req))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("no está habilitado");
    }

    @Test
    @DisplayName("Genera URL prefirmada para PRODUCT_VIDEO dentro del límite de 8 MB")
    void presignProductVideoValido() {
        PresignRequest req = new PresignRequest(MediaKind.PRODUCT_VIDEO, "video/mp4", 4_000_000L);

        PresignedUpload res = mediaService.presign(req);

        assertThat(res.uploadUrl()).isNotEmpty();
        assertThat(res.objectKey()).startsWith("videos/").endsWith(".mp4");
        assertThat(res.publicUrl()).isEqualTo("https://media.tiendasheis.com/" + res.objectKey());
        assertThat(res.headers()).containsEntry("Cache-Control", "public, max-age=31536000, immutable");
    }

    @Test
    @DisplayName("Rechaza PRODUCT_VIDEO mayor a 8 MB")
    void rechazaProductVideoMayorA8MB() {
        PresignRequest req = new PresignRequest(MediaKind.PRODUCT_VIDEO, "video/mp4", 9 * 1024 * 1024L);

        assertThatThrownBy(() -> mediaService.presign(req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("8 MB");
    }

    @Test
    @DisplayName("Rechaza formato distinto a video/mp4 para PRODUCT_VIDEO")
    void rechazaFormatoInvalidoProductVideo() {
        PresignRequest req = new PresignRequest(MediaKind.PRODUCT_VIDEO, "video/webm", 3_000_000L);

        assertThatThrownBy(() -> mediaService.presign(req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("video/mp4");
    }

    @Test
    @DisplayName("Genera URL prefirmada para RECOMMENDATION_VIDEO hasta 15 MB")
    void presignRecommendationVideoValido() {
        PresignRequest req = new PresignRequest(MediaKind.RECOMMENDATION_VIDEO, "video/mp4", 14 * 1024 * 1024L);

        PresignedUpload res = mediaService.presign(req);

        assertThat(res.objectKey()).startsWith("videos/").endsWith(".mp4");
    }

    @Test
    @DisplayName("Rechaza RECOMMENDATION_VIDEO mayor a 15 MB")
    void rechazaRecommendationVideoMayorA15MB() {
        PresignRequest req = new PresignRequest(MediaKind.RECOMMENDATION_VIDEO, "video/mp4", 16 * 1024 * 1024L);

        assertThatThrownBy(() -> mediaService.presign(req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("15 MB");
    }

    @Test
    @DisplayName("Genera URL prefirmada para POSTER webp y jpeg hasta 150 KB")
    void presignPosterValido() {
        PresignRequest reqWebp = new PresignRequest(MediaKind.POSTER, "image/webp", 100 * 1024L);
        PresignedUpload resWebp = mediaService.presign(reqWebp);
        assertThat(resWebp.objectKey()).startsWith("posters/").endsWith(".webp");

        PresignRequest reqJpeg = new PresignRequest(MediaKind.POSTER, "image/jpeg", 120 * 1024L);
        PresignedUpload resJpeg = mediaService.presign(reqJpeg);
        assertThat(resJpeg.objectKey()).startsWith("posters/").endsWith(".jpeg");
    }

    @Test
    @DisplayName("Rechaza POSTER mayor a 150 KB")
    void rechazaPosterMayorA150KB() {
        PresignRequest req = new PresignRequest(MediaKind.POSTER, "image/webp", 200 * 1024L);

        assertThatThrownBy(() -> mediaService.presign(req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("150 KB");
    }
}
