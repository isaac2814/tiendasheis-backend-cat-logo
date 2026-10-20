package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record CatalogoPublicoResponse(
        List<ServicioPublicoDto> servicios,
        List<ComboPublicoDto> combos,
        DescuentosDto descuentos,
        List<MetodoPagoDto> metodosPago,
        List<HorarioDto> horario,
        String whatsapp,
        String sobreNosotros,
        CatalogoTextosDto textos,
        List<FavoritoDto> favoritos
) {
    public CatalogoPublicoResponse(
            List<ServicioPublicoDto> servicios,
            List<ComboPublicoDto> combos,
            DescuentosDto descuentos,
            List<MetodoPagoDto> metodosPago,
            List<HorarioDto> horario,
            String whatsapp,
            String sobreNosotros
    ) {
        this(servicios, combos, descuentos, metodosPago, horario, whatsapp, sobreNosotros, null, List.of());
    }
}
