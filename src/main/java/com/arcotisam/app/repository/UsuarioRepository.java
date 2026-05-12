package com.arcotisam.app.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.arcotisam.app.model.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, UUID> {

}
