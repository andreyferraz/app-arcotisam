package com.arcotisam.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoRankingItem {

    private String nome;
    private Integer quantidadeVendida;
}
