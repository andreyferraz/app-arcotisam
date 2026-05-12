package com.arcotisam.app.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import java.util.Set;
import java.util.LinkedHashSet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("artesaos")
public class Artesao implements Persistable<UUID>{

    @Id
    private UUID id;

    private String nome;

    private String descricao;

    private String whatsapp;

    @Column("foto_url")
    private String fotoUrl;

    @Column("usuario_id")
    private UUID usuarioId;

    @MappedCollection(idColumn = "artesao_id")
    private Set<Produto> produtos = new LinkedHashSet<>();

    @Transient 
    private boolean isNew = false;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

}
