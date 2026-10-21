package com.labtbd.entity;

import java.time.OffsetDateTime;

public record OrdenTraslado(
    long id,
    long solicitudId,
    long estacionOrigenId,
    long estacionDestinoId,
    TipoVehiculo tipoVehiculo,
    int cantidad,
    long operadorId,
    OffsetDateTime creadaEn
) {}
