package com.sheis.catalogo.web.dto.publico;

public record CatalogoTextosDto(
        String bannerTexto,
        String tituloFavoritos,
        String tituloServicios,
        String tituloCombos,
        String menuFavoritos,
        String menuServicios,
        String menuCombos,
        String tituloTecnologia,
        String menuTecnologia,
        String subtituloTecnologia,
        String ciudadDomicilio,
        String textoDomicilio,
        String textoEnvioNacional,
        String notaTecnologia
) {
    public CatalogoTextosDto(
            String bannerTexto,
            String tituloFavoritos,
            String tituloServicios,
            String tituloCombos,
            String menuFavoritos,
            String menuServicios,
            String menuCombos
    ) {
        this(
                bannerTexto,
                tituloFavoritos,
                tituloServicios,
                tituloCombos,
                menuFavoritos,
                menuServicios,
                menuCombos,
                "Tecnología",
                "Tecnología",
                "Audífonos, forros, cargadores y más. Envíos a todo el país.",
                "",
                "Te lo llevamos a la puerta de tu casa.",
                "Por transportadora a cualquier ciudad. El costo te lo confirmamos por WhatsApp.",
                "Pide por WhatsApp. Domicilio en tu ciudad o envío nacional; el costo del envío te lo confirmamos en el chat."
        );
    }
}
