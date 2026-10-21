package com.labtbd.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VehiculoUpdateRequest(
    @NotBlank
    @Size(max=40)
    String codigo,

    @Min(0)
    @Max(100)
    int bateria
) {}