package com.sheis.catalogo.web.dto.publico;

import java.util.List;

public record CatalogoPublicoResponse(
        List<ServicioPublicoDto> servicios,
        List<ComboPublicoDto> combos,
        DescuentosDto descuentos,
        List<MetodoPagoDto> metodosPago,
        List<HorarioDto> horario,
        String whatsapp,
        String sobreNosotros
) {}
