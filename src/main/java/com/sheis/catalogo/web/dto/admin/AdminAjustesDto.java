package com.sheis.catalogo.web.dto.admin;

import com.sheis.catalogo.web.dto.publico.CatalogoTextosDto;
import com.sheis.catalogo.web.dto.publico.HorarioDto;
import java.util.List;

public record AdminAjustesDto(
        String whatsapp,
        String sobreNosotros,
        List<HorarioDto> horario,
        List<AdminMetodoPagoDto> metodosPago,
        CatalogoTextosDto textos
) {
    public AdminAjustesDto(String whatsapp, String sobreNosotros, List<HorarioDto> horario, List<AdminMetodoPagoDto> metodosPago) {
        this(whatsapp, sobreNosotros, horario, metodosPago, null);
    }
}
