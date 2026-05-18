package com.arcotisam.app.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.arcotisam.app.model.Movimentacao;
import com.arcotisam.app.repository.MovimentacaoRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository) {
        this.movimentacaoRepository = movimentacaoRepository;
    }

    public List<Movimentacao> listarPorArtesao(UUID artesaoId) {
        ValidationUtils.validarCampoObrigatorio(artesaoId, "artesaoId");
        return movimentacaoRepository.findByArtesaoIdOrderByDataHoraDesc(artesaoId);
    }

    public java.util.List<Movimentacao> listarPorPeriodo(UUID artesaoId, java.time.OffsetDateTime inicio, java.time.OffsetDateTime fim) {
        ValidationUtils.validarCampoObrigatorio(artesaoId, "artesaoId");
        var all = movimentacaoRepository.findByArtesaoIdOrderByDataHoraDesc(artesaoId);
        if (inicio == null && fim == null) return all;
        java.util.List<Movimentacao> out = new java.util.ArrayList<>();
        for (Movimentacao m : all) {
            var dt = m.getDataHora();
            boolean afterStart = inicio == null || !dt.isBefore(inicio);
            boolean beforeEnd = fim == null || !dt.isAfter(fim);
            if (afterStart && beforeEnd) out.add(m);
        }
        return out;
    }

    @Transactional
    public Movimentacao lancar(UUID artesaoId, String tipo, String descricao, BigDecimal valor) {
        ValidationUtils.validarCampoObrigatorio(artesaoId, "artesaoId");
        ValidationUtils.validarCampoStringObrigatorio(tipo, "tipo");
        ValidationUtils.validarCampoObrigatorio(valor, "valor");

        Movimentacao m = new Movimentacao(UUID.randomUUID(), artesaoId, tipo, descricao, valor, OffsetDateTime.now(), true);
        return movimentacaoRepository.save(m);
    }

}
