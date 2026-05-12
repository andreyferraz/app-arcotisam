package com.arcotisam.app.repository;

import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.arcotisam.app.model.Produto;

public interface ProdutoRepository extends CrudRepository<Produto, UUID> {

}
