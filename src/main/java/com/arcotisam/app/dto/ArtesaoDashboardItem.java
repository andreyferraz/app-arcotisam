package com.arcotisam.app.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtesaoDashboardItem {

    private UUID id;
    private String nome;
    private String descricao;
    private String whatsapp;
    private String fotoUrl;
    private String username;
    private Integer totalProdutos;
}
