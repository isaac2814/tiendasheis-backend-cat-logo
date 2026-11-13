package com.sheis.catalogo.service.media;

import com.sheis.catalogo.domain.CategoriaProducto;
import com.sheis.catalogo.domain.Producto;
import com.sheis.catalogo.domain.ProductoVideo;
import com.sheis.catalogo.exception.ResourceNotFoundException;
import com.sheis.catalogo.exception.UnprocessableEntityException;
import com.sheis.catalogo.repository.ProductoRepository;
import com.sheis.catalogo.repository.ProductoVideoRepository;
import com.sheis.catalogo.web.dto.admin.GuardarVideoProductoRequest;
import com.sheis.catalogo.web.dto.admin.ProductoVideoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoVideoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoVideoRepository productoVideoRepository;

    private InMemoryMediaStorage storage;
    private ProductoVideoService service;
    private Producto productoPrueba;

    @BeforeEach
    void setUp() {
        storage = new InMemoryMediaStorage("https://media.tiendasheis.com");
        service = new ProductoVideoService(productoRepository, productoVideoRepository, storage);

        CategoriaProducto cat = new CategoriaProducto("Audífonos", 1, true);
        productoPrueba = new Producto(cat, "Audífonos Pro", "Lema", "Desc", "Incluye", "Garantía", "Color", 1, true);
        productoPrueba.setId(10L);
    }

    @Test
    @DisplayName("Guarda video exitosamente cuando head() valida tipo y tamaño y duración <= 20s")
    void guardaVideoExitosamente() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoPrueba));
        when(productoVideoRepository.findByProductoId(10L)).thenReturn(Optional.empty());
        when(productoVideoRepository.save(any(ProductoVideo.class))).thenAnswer(inv -> {
            ProductoVideo pv = inv.getArgument(0);
            pv.setId(100L);
            return pv;
        });

        storage.putMockObject("videos/abc.mp4", "video/mp4", 5_000_000L);
        storage.putMockObject("posters/abc.webp", "image/webp", 80_000L);

        GuardarVideoProductoRequest req = new GuardarVideoProductoRequest("videos/abc.mp4", "posters/abc.webp", 18);

        ProductoVideoDto dto = service.guardarVideo(10L, req);

        assertThat(dto).isNotNull();
        assertThat(dto.productoId()).isEqualTo(10L);
        assertThat(dto.duracionSeg()).isEqualTo(18);
        assertThat(dto.videoUrl()).isEqualTo("https://media.tiendasheis.com/videos/abc.mp4");
        assertThat(dto.posterUrl()).isEqualTo("https://media.tiendasheis.com/posters/abc.webp");
    }

    @Test
    @DisplayName("Rechaza video si duración excede 20 segundos y limpia objetos subidos")
    void rechazaDuracionMayorA20Segundos() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoPrueba));

        storage.putMockObject("videos/abc.mp4", "video/mp4", 5_000_000L);
        storage.putMockObject("posters/abc.webp", "image/webp", 80_000L);

        GuardarVideoProductoRequest req = new GuardarVideoProductoRequest("videos/abc.mp4", "posters/abc.webp", 25);

        assertThatThrownBy(() -> service.guardarVideo(10L, req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("20 segundos");

        // Verifica que los objetos fueron limpiados del storage
        assertThat(storage.head("videos/abc.mp4")).isEmpty();
        assertThat(storage.head("posters/abc.webp")).isEmpty();
    }

    @Test
    @DisplayName("Rechaza si el objeto de video no existe en storage")
    void rechazaSiVideoNoExisteEnStorage() {
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoPrueba));

        GuardarVideoProductoRequest req = new GuardarVideoProductoRequest("videos/noexiste.mp4", "posters/noexiste.webp", 15);

        assertThatThrownBy(() -> service.guardarVideo(10L, req))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("no se encuentra en el almacenamiento");
    }

    @Test
    @DisplayName("Reemplazar video existente borra el video y portada viejos de storage")
    void reemplazoDeVideoBorraAnterior() {
        ProductoVideo videoViejo = new ProductoVideo(productoPrueba, "videos/viejo.mp4", "posters/viejo.webp", 12, 4_000_000L);
        storage.putMockObject("videos/viejo.mp4", "video/mp4", 4_000_000L);
        storage.putMockObject("posters/viejo.webp", "image/webp", 50_000L);

        storage.putMockObject("videos/nuevo.mp4", "video/mp4", 6_000_000L);
        storage.putMockObject("posters/nuevo.webp", "image/webp", 60_000L);

        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoPrueba));
        when(productoVideoRepository.findByProductoId(10L)).thenReturn(Optional.of(videoViejo));
        when(productoVideoRepository.save(any(ProductoVideo.class))).thenAnswer(inv -> inv.getArgument(0));

        GuardarVideoProductoRequest req = new GuardarVideoProductoRequest("videos/nuevo.mp4", "posters/nuevo.webp", 15);

        service.guardarVideo(10L, req);

        // Los viejos deben haber sido eliminados del storage
        assertThat(storage.head("videos/viejo.mp4")).isEmpty();
        assertThat(storage.head("posters/viejo.webp")).isEmpty();
        // Los nuevos deben conservarse
        assertThat(storage.head("videos/nuevo.mp4")).isPresent();
        assertThat(storage.head("posters/nuevo.webp")).isPresent();
    }

    @Test
    @DisplayName("Eliminar video de producto lo quita de storage y de repository")
    void eliminarVideoBorraDeStorageYDb() {
        ProductoVideo video = new ProductoVideo(productoPrueba, "videos/temp.mp4", "posters/temp.webp", 10, 3_000_000L);
        storage.putMockObject("videos/temp.mp4", "video/mp4", 3_000_000L);
        storage.putMockObject("posters/temp.webp", "image/webp", 40_000L);

        when(productoVideoRepository.findByProductoId(10L)).thenReturn(Optional.of(video));

        service.eliminarVideo(10L);

        assertThat(storage.head("videos/temp.mp4")).isEmpty();
        assertThat(storage.head("posters/temp.webp")).isEmpty();
        verify(productoVideoRepository).delete(video);
    }
}
