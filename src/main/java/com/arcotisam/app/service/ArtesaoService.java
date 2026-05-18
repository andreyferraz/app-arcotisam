package com.arcotisam.app.service;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import com.arcotisam.app.model.Produto;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.arcotisam.app.model.Artesao;
import com.arcotisam.app.repository.ArtesaoRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class ArtesaoService {

    private final ArtesaoRepository artesaoRepository;
    private final FileUploadService fileUploadService;

    public ArtesaoService(ArtesaoRepository artesaoRepository, FileUploadService fileUploadService) {
        this.artesaoRepository = artesaoRepository;
        this.fileUploadService = fileUploadService;
    }

    public Optional<Artesao> buscarPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        return artesaoRepository.findById(id);
    }

    public Optional<Artesao> buscarPorUsuarioId(UUID usuarioId) {
        ValidationUtils.validarCampoObrigatorio(usuarioId, "usuarioId");
        return artesaoRepository.findByUsuarioId(usuarioId);
    }

    @Transactional
    public Artesao criarArtesao(String nome, String descricao, String whatsapp, UUID usuarioId, MultipartFile foto) {
        ValidationUtils.validarCampoStringObrigatorio(nome, "nome");
        ValidationUtils.validarCampoObrigatorio(usuarioId, "usuarioId");

        Set<Produto> produtosVazios = new LinkedHashSet<>();
        Artesao novo = new Artesao(UUID.randomUUID(), nome, descricao, whatsapp, null, usuarioId, produtosVazios, true);

        if (foto != null && !foto.isEmpty()) {
            String savedName = fileUploadService.salvarImagem(foto);
            novo.setFotoUrl(savedName);
        }

        return artesaoRepository.save(novo);
    }

    @Transactional
    public Artesao atualizar(UUID id, String nome, String descricao, String whatsapp, MultipartFile foto) {
        ValidationUtils.validarCampoObrigatorio(id, "id");

        Artesao existente = artesaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artesao nao encontrado."));

        if (nome != null) existente.setNome(nome);
        if (descricao != null) existente.setDescricao(descricao);
        if (whatsapp != null) existente.setWhatsapp(whatsapp);

        if (foto != null && !foto.isEmpty()) {
            if (existente.getFotoUrl() != null && !existente.getFotoUrl().isEmpty()) {
                try { fileUploadService.removerImagem(existente.getFotoUrl()); } catch (Exception e) { /* ignore */ }
            }
            String savedName = fileUploadService.salvarImagem(foto);
            existente.setFotoUrl(savedName);
        }

        return artesaoRepository.save(existente);
    }

    @Transactional
    public void deletar(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        var opt = artesaoRepository.findById(id);
        if (opt.isPresent()) {
            var a = opt.get();
            if (a.getFotoUrl() != null && !a.getFotoUrl().isEmpty()) {
                try { fileUploadService.removerImagem(a.getFotoUrl()); } catch (Exception e) { /* ignore */ }
            }
        }
        artesaoRepository.deleteById(id);
    }

}
