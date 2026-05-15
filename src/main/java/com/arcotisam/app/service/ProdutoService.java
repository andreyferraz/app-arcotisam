package com.arcotisam.app.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.arcotisam.app.model.Produto;
import com.arcotisam.app.repository.ArtesaoRepository;
import com.arcotisam.app.repository.ProdutoRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ArtesaoRepository artesaoRepository;
    private final FileUploadService fileUploadService;

    public ProdutoService(ProdutoRepository produtoRepository, ArtesaoRepository artesaoRepository, FileUploadService fileUploadService) {
        this.produtoRepository = produtoRepository;
        this.artesaoRepository = artesaoRepository;
        this.fileUploadService = fileUploadService;
    }

    public Optional<Produto> buscarPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        return produtoRepository.findById(id);
    }

    @Transactional
    public Produto criarProduto(String nome, String descricao, BigDecimal preco, UUID artesaoId, MultipartFile imagem) {
        ValidationUtils.validarCampoStringObrigatorio(nome, "nome");
        ValidationUtils.validarCampoObrigatorio(artesaoId, "artesaoId");

        if (artesaoRepository.findById(artesaoId).isEmpty()) {
            throw new IllegalArgumentException("Artesao nao encontrado: " + artesaoId);
        }

        Produto novo = new Produto(UUID.randomUUID(), nome, descricao, preco, null, true, 0, artesaoId, true);

        if (imagem != null && !imagem.isEmpty()) {
            String savedName = fileUploadService.salvarImagem(imagem);
            novo.setImagemUrl(savedName);
        }

        return produtoRepository.save(novo);
    }

    @Transactional
    public Produto atualizar(UUID id, String nome, String descricao, BigDecimal preco, MultipartFile imagem) {
        ValidationUtils.validarCampoObrigatorio(id, "id");

        Produto existente = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));

        if (nome != null) existente.setNome(nome);
        if (descricao != null) existente.setDescricao(descricao);
        if (preco != null) existente.setPreco(preco);

        if (imagem != null && !imagem.isEmpty()) {
            if (existente.getImagemUrl() != null && !existente.getImagemUrl().isEmpty()) {
                try { fileUploadService.removerImagem(existente.getImagemUrl()); } catch (Exception e) { /* ignore */ }
            }
            String savedName = fileUploadService.salvarImagem(imagem);
            existente.setImagemUrl(savedName);
        }

        return produtoRepository.save(existente);
    }

    @Transactional
    public void deletar(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        var opt = produtoRepository.findById(id);
        if (opt.isPresent()) {
            var p = opt.get();
            if (p.getImagemUrl() != null && !p.getImagemUrl().isEmpty()) {
                try { fileUploadService.removerImagem(p.getImagemUrl()); } catch (Exception e) { /* ignore */ }
            }
        }
        produtoRepository.deleteById(id);
    }

}
