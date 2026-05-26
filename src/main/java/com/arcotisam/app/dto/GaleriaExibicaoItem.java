package com.arcotisam.app.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GaleriaExibicaoItem {

    private UUID id;
    private String titulo;
    private List<String> fotos;
}