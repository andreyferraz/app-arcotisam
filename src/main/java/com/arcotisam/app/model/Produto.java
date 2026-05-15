package com.arcotisam.app.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("produtos")
public class Produto implements Persistable<UUID> {

    @Id
    private UUID id;

    private String nome;

    private String descricao;

    private BigDecimal preco;

    @Column("imagem_url")
    private String imagemUrl;

    private Boolean ativo;

    @Column("quantidade_vendida")
    private Integer quantidadeVendida;

    @Column("artesao_id")
    private UUID artesaoId;

    @Transient
    private boolean isNew = false;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

}
