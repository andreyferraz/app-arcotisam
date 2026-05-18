package com.arcotisam.app.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.arcotisam.app.model.Movimentacao;

public interface MovimentacaoRepository extends CrudRepository<Movimentacao, UUID> {

    List<Movimentacao> findByArtesaoIdOrderByDataHoraDesc(UUID artesaoId);

}
