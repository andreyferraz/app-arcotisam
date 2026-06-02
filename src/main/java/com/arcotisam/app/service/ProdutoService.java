package com.arcotisam.app.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    private static final String CAMPO_ID = "id";
    private static final String CAMPO_NOME = "nome";
    private static final String CAMPO_DESCRICAO = "descricao";
    private static final String CAMPO_PRECO = "preco";
    private static final String CAMPO_IMAGEM_URL = "imagem_url";
    private static final String CAMPO_ATIVO = "ativo";
    private static final String CAMPO_QUANTIDADE_VENDIDA_DB = "quantidade_vendida";
    private static final String CAMPO_ARTESAO_ID = "artesaoId";
    private static final String CAMPO_ARTESAO_ID_DB = "artesao_id";
    private static final String CAMPO_QUANTIDADE_VENDIDA = "quantidadeVendida";
    private static final String MENSAGEM_ID_NAO_NULO = "Produto id nao pode ser nulo";

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
        ValidationUtils.validarCampoObrigatorio(artesaoId, CAMPO_ARTESAO_ID);
        return produtoRepository.findByArtesaoId(artesaoId);
    }

    public List<Produto> listarTodos() {
        return StreamSupport.stream(produtoRepository.findAll().spliterator(), false)
                .toList();
    }

    public List<Produto> listarUltimosCadastrados(int limite) {
        int limiteSeguro = Math.max(1, limite);

        return namedParameterJdbcTemplate.query(
                "SELECT id, nome, descricao, preco, imagem_url, ativo, quantidade_vendida, artesao_id " +
                "FROM produtos ORDER BY rowid DESC LIMIT :limite",
                new MapSqlParameterSource().addValue("limite", limiteSeguro),
                (rs, rowNum) -> {
                    Integer quantidadeVendida = rs.getInt(CAMPO_QUANTIDADE_VENDIDA_DB);
                    if (rs.wasNull()) {
                        quantidadeVendida = null;
                    }

                        return new Produto(
                            UUID.fromString(rs.getString("id")),
                            rs.getString(CAMPO_NOME),
                            rs.getString(CAMPO_DESCRICAO),
                            rs.getBigDecimal(CAMPO_PRECO),
                            rs.getString(CAMPO_IMAGEM_URL),
                            rs.getInt(CAMPO_ATIVO) != 0,
                            quantidadeVendida,
                            UUID.fromString(rs.getString(CAMPO_ARTESAO_ID_DB)),
                            false);
                });
    }

    @Transactional
    public Produto criarProduto(String nome, String descricao, BigDecimal preco, UUID artesaoId, MultipartFile imagem) {
        ValidationUtils.validarCampoStringObrigatorio(nome, "nome");
        ValidationUtils.validarCampoObrigatorio(artesaoId, CAMPO_ARTESAO_ID);

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
                .addValue(CAMPO_ID, Objects.requireNonNull(novo.getId(), MENSAGEM_ID_NAO_NULO).toString())
                .addValue(CAMPO_NOME, novo.getNome())
                .addValue(CAMPO_DESCRICAO, novo.getDescricao())
                .addValue(CAMPO_PRECO, novo.getPreco())
                .addValue("imagemUrl", novo.getImagemUrl())
                .addValue(CAMPO_ATIVO, Boolean.TRUE.equals(novo.getAtivo()) ? 1 : 0)
                .addValue(CAMPO_QUANTIDADE_VENDIDA, novo.getQuantidadeVendida())
                .addValue(CAMPO_ARTESAO_ID, novo.getArtesaoId().toString())
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
                .addValue(CAMPO_ID, Objects.requireNonNull(existente.getId(), MENSAGEM_ID_NAO_NULO).toString())
                .addValue("nome", existente.getNome())
                .addValue(CAMPO_DESCRICAO, existente.getDescricao())
                .addValue(CAMPO_PRECO, existente.getPreco())
                .addValue("imagemUrl", existente.getImagemUrl())
                .addValue(CAMPO_ATIVO, Boolean.TRUE.equals(existente.getAtivo()) ? 1 : 0)
                .addValue(CAMPO_QUANTIDADE_VENDIDA, existente.getQuantidadeVendida())
                .addValue(CAMPO_ARTESAO_ID, existente.getArtesaoId().toString())
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
            new MapSqlParameterSource().addValue(CAMPO_ID, id.toString()));
    }

    @Transactional
    public String registrarCliqueEObterLinkWhatsApp(UUID produtoId) {
        ValidationUtils.validarCampoObrigatorio(produtoId, "produtoId");

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new IllegalArgumentException("Produto nao encontrado."));

        if (produto.getArtesaoId() == null) {
            throw new IllegalArgumentException("Produto sem artesao vinculado.");
        }

        var artesao = artesaoRepository.findById(produto.getArtesaoId())
                .orElseThrow(() -> new IllegalArgumentException("Artesao nao encontrado."));

        String whatsapp = normalizarWhatsapp(artesao.getWhatsapp());
        if (whatsapp == null) {
            throw new IllegalArgumentException("Artesao sem WhatsApp cadastrado.");
        }

        int novaQuantidade = Optional.ofNullable(produto.getQuantidadeVendida()).orElse(0) + 1;
        namedParameterJdbcTemplate.update(
                "UPDATE produtos SET quantidade_vendida = :quantidadeVendida WHERE id = :id",
                new MapSqlParameterSource()
            .addValue(CAMPO_ID, Objects.requireNonNull(produto.getId(), MENSAGEM_ID_NAO_NULO).toString())
                .addValue(CAMPO_QUANTIDADE_VENDIDA, novaQuantidade));

        String mensagem = String.format(
                "Olá! Tenho interesse no produto %s publicado na ARCOTISAM.",
                produto.getNome());

        return "https://wa.me/" + whatsapp + "?text=" + URLEncoder.encode(mensagem, StandardCharsets.UTF_8);
    }

    private String normalizarWhatsapp(String whatsapp) {
        if (whatsapp == null) {
            return null;
        }

        String numeros = whatsapp.trim().replaceAll("\\D", "");
        if (numeros.isEmpty()) {
            return null;
        }

        if (!numeros.startsWith("55")) {
            numeros = "55" + numeros;
        }

        return numeros;
    }

}
