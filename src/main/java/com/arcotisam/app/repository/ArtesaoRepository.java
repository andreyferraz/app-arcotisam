package com.arcotisam.app.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.arcotisam.app.model.Artesao;

public interface ArtesaoRepository extends CrudRepository<Artesao, UUID> {

	java.util.Optional<Artesao> findByUsuarioId(UUID usuarioId);

}
