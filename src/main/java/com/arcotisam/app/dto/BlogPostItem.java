package com.arcotisam.app.dto;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogPostItem {

    private UUID id;
    private String titulo;
    private LocalDate dataPublicacao;
    private String fotoUrl;
    private String conteudoHtml;
}
