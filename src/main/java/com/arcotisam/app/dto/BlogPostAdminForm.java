package com.arcotisam.app.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostAdminForm {

    private UUID id;
    private String titulo;
    private String dataPublicacao;
    private String fotoUrlAtual;
    private String fotoCapaAtual;
    private List<String> fotosAtuais;
    private String conteudoHtml;
}
