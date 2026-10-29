package com.labtbd.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record Viaje(
    long id,
    long usuarioId,
    long vehiculoId,
    OffsetDateTime iniciadoEn,
    OffsetDateTime finalizadoEn,
    BigDecimal costo
) {}