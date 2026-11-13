package com.sheis.catalogo.service.media;

import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.Recomendacion;
import com.sheis.catalogo.domain.TipoRecomendacion;
import com.sheis.catalogo.exception.UnprocessableEntityException;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.RecomendacionRepository;
import com.sheis.catalogo.web.dto.admin.ActualizarRecomendacionRequest;
import com.sheis.catalogo.web.dto.admin.AdminRecomendacionDto;
import com.sheis.catalogo.web.dto.admin.CrearRecomendacionRequest;
import com.sheis.catalogo.web.dto.publico.PublicRecomendacionesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecomendacionServiceTest {

    @Mock
    private RecomendacionRepository recomendacionRepository;

    @Mock
    private ProductoRepository productoRepository;

    private InMemoryMediaStorage storage;
    private RecomendacionService service;

    @BeforeEach
    void setUp() {
        storage = new InMemoryMediaStorage("https://media.tiendasheis.com");
        service = new RecomendacionService(recomendacionRepository, productoRepository, storage);
    }

    @Test
    @DisplayName("Crea recomendación de tipo CLIP validando objetos y duración <= 45s")
    void crearClipExitoso() {
        storage.putMockObject("videos/clip1.mp4", "video/mp4", 10_000_000L);
        storage.putMockObject("posters/clip1.webp", "image/webp", 90_000L);

        when(recomendacionRepository.findTopByOrderByOrdenDesc()).thenReturn(Optional.empty());
        when(recomendacionRepository.save(any(Recomendacion.class))).thenAnswer(inv -> {
            Recomendacion r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        CrearRecomendacionRequest req = new CrearRecomendacionRequest(
                "Cómo configurar Netflix",
                "Paso a paso sencillo",
                TipoRecomendacion.CLIP,
                "videos/clip1.mp4",
                "posters/clip1.webp",
                null,
                30,
                null,
                "audifonos",
                null,
                true
        );

        AdminRecomendacionDto res = service.crear(req);

        assertThat(res).isNotNull();
        assertThat(res.tipo()).isEqualTo(TipoRecomendacion.CLIP);
        assertThat(res.videoUrl()).isEqualTo("https://media.tiendasheis.com/videos/clip1.mp4");
        assertThat(res.duracionSeg()).isEqualTo(30);
    }

    @Test
    @DisplayName("Rechaza CLIP si duración excede 45 segundos y borra archivos")
    void rechazaClipMayorA45Segundos() {
        storage.putMockObject("videos/largo.mp4", "video/mp4", 10_000_000L);
        storage.putMockObject("posters/largo.webp", "image/webp", 90_000L);

        CrearRecomendacionRequest req = new CrearRecomendacionRequest(
                "Clip muy largo",
                null,
                TipoRecomendacion.CLIP,
                "videos/largo.mp4",
                "posters/largo.webp",
                null,
                50,
                null,
                null,
                1,
                true
        );

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("45 segundos");

        assertThat(storage.head("videos/largo.mp4")).isEmpty();
        assertThat(storage.head("posters/largo.webp")).isEmpty();
    }

    @Test
    @DisplayName("Crea recomendación de tipo YOUTUBE extrayendo id de 11 caracteres")
    void crearYouTubeExitoso() {
        when(recomendacionRepository.findTopByOrderByOrdenDesc()).thenReturn(Optional.empty());
        when(recomendacionRepository.save(any(Recomendacion.class))).thenAnswer(inv -> {
            Recomendacion r = inv.getArgument(0);
            r.setId(2L);
            return r;
        });

        CrearRecomendacionRequest req = new CrearRecomendacionRequest(
                "Tutorial completo YouTube",
                "Mira este video",
                TipoRecomendacion.YOUTUBE,
                null,
                null,
                "https://youtu.be/dQw4w9WgXcQ",
                40,
                null,
                null,
                null,
                true
        );

        AdminRecomendacionDto res = service.crear(req);

        assertThat(res).isNotNull();
        assertThat(res.tipo()).isEqualTo(TipoRecomendacion.YOUTUBE);
        assertThat(res.youtubeId()).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    @DisplayName("Rechaza recomendación YOUTUBE si enlace es inválido")
    void rechazaYouTubeInvalido() {
        CrearRecomendacionRequest req = new CrearRecomendacionRequest(
                "Video inválido",
                null,
                TipoRecomendacion.YOUTUBE,
                null,
                null,
                "https://notyoutube.com/watch",
                30,
                null,
                null,
                1,
                true
        );

        assertThatThrownBy(() -> service.crear(req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("no válido");
    }

    @Test
    @DisplayName("Actualizar recomendación de CLIP a YOUTUBE elimina objetos de storage")
    void cambioDeClipAYouTubeLimpiaStorage() {
        storage.putMockObject("videos/old.mp4", "video/mp4", 5_000_000L);
        storage.putMockObject("posters/old.webp", "image/webp", 40_000L);

        Recomendacion rec = new Recomendacion(
                "Título", "Desc", TipoRecomendacion.CLIP,
                "videos/old.mp4", "posters/old.webp", null,
                25, 5_000_000L, null, null, 1, true
        );
        rec.setId(5L);

        when(recomendacionRepository.findById(5L)).thenReturn(Optional.of(rec));
        when(recomendacionRepository.save(any(Recomendacion.class))).thenAnswer(inv -> inv.getArgument(0));

        ActualizarRecomendacionRequest req = new ActualizarRecomendacionRequest(
                "Ahora es YouTube",
                "Nueva desc",
                TipoRecomendacion.YOUTUBE,
                null,
                null,
                "dQw4w9WgXcQ",
                30,
                null,
                null,
                1,
                true
        );

        AdminRecomendacionDto res = service.actualizar(5L, req);

        assertThat(res.tipo()).isEqualTo(TipoRecomendacion.YOUTUBE);
        assertThat(res.youtubeId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(storage.head("videos/old.mp4")).isEmpty();
        assertThat(storage.head("posters/old.webp")).isEmpty();
    }

    @Test
    @DisplayName("Eliminar recomendación CLIP borra objetos del storage y entidad")
    void eliminarBorraDeStorageYDb() {
        storage.putMockObject("videos/del.mp4", "video/mp4", 5_000_000L);
        storage.putMockObject("posters/del.webp", "image/webp", 40_000L);

        Recomendacion rec = new Recomendacion(
                "Por borrar", null, TipoRecomendacion.CLIP,
                "videos/del.mp4", "posters/del.webp", null,
                15, 5_000_000L, null, null, 1, true
        );
        rec.setId(8L);

        when(recomendacionRepository.findById(8L)).thenReturn(Optional.of(rec));

        service.eliminar(8L);

        assertThat(storage.head("videos/del.mp4")).isEmpty();
        assertThat(storage.head("posters/del.webp")).isEmpty();
        verify(recomendacionRepository).delete(rec);
    }

    @Test
    @DisplayName("Reordenar actualiza orden secuencialmente según lista de IDs")
    void reordenarActualizaOrden() {
        Recomendacion r1 = new Recomendacion("R1", null, TipoRecomendacion.YOUTUBE, null, null, "dQw4w9WgXcQ", 10, null, null, null, 1, true);
        r1.setId(10L);
        Recomendacion r2 = new Recomendacion("R2", null, TipoRecomendacion.YOUTUBE, null, null, "dQw4w9WgXcQ", 10, null, null, null, 2, true);
        r2.setId(20L);

        when(recomendacionRepository.findById(20L)).thenReturn(Optional.of(r2));
        when(recomendacionRepository.findById(10L)).thenReturn(Optional.of(r1));

        service.reordenar(List.of(20L, 10L));

        assertThat(r2.getOrden()).isEqualTo(1);
        assertThat(r1.getOrden()).isEqualTo(2);
        verify(recomendacionRepository).save(r2);
        verify(recomendacionRepository).save(r1);
    }

    @Test
    @DisplayName("Consulta pública filtra solo recomendaciones activas paginadas")
    void consultaPublicaRetornaSoloActivas() {
        Recomendacion r1 = new Recomendacion("R1", null, TipoRecomendacion.YOUTUBE, null, null, "dQw4w9WgXcQ", 10, null, null, null, 1, true);
        r1.setId(10L);

        when(recomendacionRepository.findPublicas(eq("audifonos"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r1), PageRequest.of(0, 8), 1));

        PublicRecomendacionesResponse res = service.obtenerPublicas("audifonos", null, 0, 8);

        assertThat(res.items()).hasSize(1);
        assertThat(res.items().get(0).titulo()).isEqualTo("R1");
        assertThat(res.totalElements()).isEqualTo(1);
    }
}
