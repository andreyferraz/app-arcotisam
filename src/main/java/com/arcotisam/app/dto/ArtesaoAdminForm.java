package com.arcotisam.app.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtesaoAdminForm {

    private UUID id;
    private String nome;
    private String descricao;
    private String whatsapp;
    private String username;
    private String password;
    private String fotoUrlAtual;
}
