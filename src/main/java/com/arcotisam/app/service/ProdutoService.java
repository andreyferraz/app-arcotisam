package com.arcotisam.app.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public ProdutoService(ProdutoRepository produtoRepository, ArtesaoRepository artesaoRepository, FileUploadService fileUploadService, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.produtoRepository = produtoRepository;
        this.artesaoRepository = artesaoRepository;
        this.fileUploadService = fileUploadService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Optional<Produto> buscarPorId(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        return produtoRepository.findById(id);
    }

    public java.util.List<Produto> listarPorArtesao(UUID artesaoId) {
        ValidationUtils.validarCampoObrigatorio(artesaoId, "artesaoId");
        return produtoRepository.findByArtesaoId(artesaoId);
    }

    public List<Produto> listarTodos() {
        return StreamSupport.stream(produtoRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
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

        namedParameterJdbcTemplate.update(
            "INSERT INTO produtos (id, nome, descricao, preco, imagem_url, ativo, quantidade_vendida, artesao_id) " +
            "VALUES (:id, :nome, :descricao, :preco, :imagemUrl, :ativo, :quantidadeVendida, :artesaoId)",
            new MapSqlParameterSource()
                .addValue("id", novo.getId().toString())
                .addValue("nome", novo.getNome())
                .addValue("descricao", novo.getDescricao())
                .addValue("preco", novo.getPreco())
                .addValue("imagemUrl", novo.getImagemUrl())
                .addValue("ativo", Boolean.TRUE.equals(novo.getAtivo()) ? 1 : 0)
                .addValue("quantidadeVendida", novo.getQuantidadeVendida())
                .addValue("artesaoId", novo.getArtesaoId().toString())
        );

        return novo;
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

        namedParameterJdbcTemplate.update(
            "UPDATE produtos SET nome = :nome, descricao = :descricao, preco = :preco, " +
            "imagem_url = :imagemUrl, ativo = :ativo, quantidade_vendida = :quantidadeVendida, artesao_id = :artesaoId " +
            "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("id", existente.getId().toString())
                .addValue("nome", existente.getNome())
                .addValue("descricao", existente.getDescricao())
                .addValue("preco", existente.getPreco())
                .addValue("imagemUrl", existente.getImagemUrl())
                .addValue("ativo", Boolean.TRUE.equals(existente.getAtivo()) ? 1 : 0)
                .addValue("quantidadeVendida", existente.getQuantidadeVendida())
                .addValue("artesaoId", existente.getArtesaoId().toString())
        );

        return existente;
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
        namedParameterJdbcTemplate.update(
            "DELETE FROM produtos WHERE id = :id",
            new MapSqlParameterSource().addValue("id", id.toString()));
    }

}
