package com.arcotisam.app.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
@Table("movimentacoes")
public class Movimentacao implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("artesao_id")
    private UUID artesaoId;

    private String tipo; // ENTRADA | SAIDA

    private String descricao;

    private BigDecimal valor;

    @Column("data_hora")
    private OffsetDateTime dataHora;

    @Transient
    private boolean isNew = false;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

}
