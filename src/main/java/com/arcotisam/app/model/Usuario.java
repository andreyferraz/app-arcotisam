package com.arcotisam.app.model;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import com.arcotisam.app.enuns.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("usuarios")
public class Usuario implements Persistable<UUID>{

    @Id
    private UUID id;

    private String username;

    private String password;

    private String fotoUrl;
    
    private Role role;

    @Transient 
    private boolean isNew = false;

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

}
