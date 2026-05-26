package com.arcotisam.app.service;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.arcotisam.app.dto.ArtesaoAdminForm;
import com.arcotisam.app.dto.ArtesaoDashboardItem;
import com.arcotisam.app.dto.BlogPostAdminForm;
import com.arcotisam.app.dto.BlogPostItem;
import com.arcotisam.app.dto.GaleriaExibicaoItem;
import com.arcotisam.app.dto.ProdutoRankingItem;
import com.arcotisam.app.enuns.Role;
import com.arcotisam.app.model.Artesao;
import com.arcotisam.app.model.Produto;
import com.arcotisam.app.model.Usuario;
import com.arcotisam.app.repository.ArtesaoRepository;
import com.arcotisam.app.repository.ProdutoRepository;
import com.arcotisam.app.repository.UsuarioRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class AdminMasterService {

    private static final String ARTESAO_NAO_ENCONTRADO = "Artesao nao encontrado.";
    private static final String CAMPO_ID = "id";
    private static final String CAMPO_ARTESAO_ID = "artesaoId";
    private static final String CAMPO_USUARIO_ID = "usuarioId";
    private static final String CAMPO_TITULO = "titulo";
    private static final String CAMPO_DATA_PUBLICACAO = "dataPublicacao";
    private static final String CAMPO_DATA_PUBLICACAO_DB = "data_publicacao";
    private static final String CAMPO_CONTEUDO_HTML = "conteudoHtml";
    private static final String CAMPO_CONTEUDO_HTML_DB = "conteudo_html";
    private static final String CAMPO_FOTO_URL = "fotoUrl";
    private static final String CAMPO_CHAVE = "chave";
    private static final String CAMPO_VALOR = "valor";
    private static final String CONFIG_FOTO_ASSOCIACAO = "foto_associacao";
    private static final String ARTESAO_SEM_USUARIO_VINCULADO = "Artesao sem usuario vinculado.";
    private static final String POSTAGEM_NAO_ENCONTRADA = "Postagem nao encontrada.";

    private final ArtesaoRepository artesaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final UsuarioService usuarioService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public AdminMasterService(
            ArtesaoRepository artesaoRepository,
            UsuarioRepository usuarioRepository,
            ProdutoRepository produtoRepository,
            PasswordEncoder passwordEncoder,
            FileUploadService fileUploadService,
            UsuarioService usuarioService,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.artesaoRepository = artesaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.usuarioService = usuarioService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public ArtesaoAdminForm carregarFormulario(UUID id) {
        ArtesaoAdminForm form = new ArtesaoAdminForm();

        if (id == null) {
            return form;
        }

        Artesao artesao = artesaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ARTESAO_NAO_ENCONTRADO));

        form.setId(artesao.getId());
        form.setNome(artesao.getNome());
        form.setDescricao(artesao.getDescricao());
        form.setWhatsapp(artesao.getWhatsapp());
        form.setFotoUrlAtual(artesao.getFotoUrl());

        if (artesao.getUsuarioId() != null) {
            usuarioRepository.findById(artesao.getUsuarioId())
                    .ifPresent(usuario -> form.setUsername(usuario.getUsername()));
        }

        return form;
    }

    public List<ArtesaoDashboardItem> listarArtesaos() {
        Map<UUID, Integer> totalProdutosPorArtesao = contarProdutosPorArtesao();

        return StreamSupport.stream(artesaoRepository.findAll().spliterator(), false)
                .map(artesao -> toDashboardItem(artesao, totalProdutosPorArtesao))
                .sorted(Comparator.comparing(item -> Optional.ofNullable(item.getNome()).orElse(""), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<ProdutoRankingItem> listarProdutosMaisVendidos(int limite) {
        return StreamSupport.stream(produtoRepository.findAll().spliterator(), false)
                .sorted(Comparator.comparingInt(this::quantidadeVendida).reversed())
                .limit(limite)
                .map(produto -> new ProdutoRankingItem(produto.getNome(), quantidadeVendida(produto)))
                .toList();
    }

    public long contarArtesaos() {
        return StreamSupport.stream(artesaoRepository.findAll().spliterator(), false).count();
    }

    public long contarProdutos() {
        return StreamSupport.stream(produtoRepository.findAll().spliterator(), false).count();
    }

    public long contarVendas() {
        return StreamSupport.stream(produtoRepository.findAll().spliterator(), false)
                .mapToLong(this::quantidadeVendida)
                .sum();
    }

    public List<GaleriaExibicaoItem> listarGalerias() {
        List<GaleriaExibicaoItem> galerias = namedParameterJdbcTemplate.query(
            "SELECT id, titulo FROM galerias ORDER BY rowid DESC",
                new MapSqlParameterSource(),
                (rs, rowNum) -> new GaleriaExibicaoItem(
                UUID.fromString(rs.getString(CAMPO_ID)),
                rs.getString(CAMPO_TITULO),
                        new ArrayList<>())
        );

        for (GaleriaExibicaoItem galeria : galerias) {
            List<String> fotos = namedParameterJdbcTemplate.query(
                    "SELECT arquivo_url FROM galeria_fotos WHERE galeria_id = :galeriaId ORDER BY ordem ASC, rowid ASC",
                    new MapSqlParameterSource().addValue("galeriaId", galeria.getId().toString()),
                (rs, rowNum) -> rs.getString("arquivo_url"));
            galeria.setFotos(fotos);
        }

        return galerias;
    }

    public List<BlogPostItem> listarBlogPosts() {
        return namedParameterJdbcTemplate.query(
            "SELECT id, titulo, data_publicacao, foto_url, conteudo_html FROM blog_posts ORDER BY data_publicacao DESC, rowid DESC",
                new MapSqlParameterSource(),
                (rs, rowNum) -> new BlogPostItem(
                        UUID.fromString(rs.getString(CAMPO_ID)),
                        rs.getString(CAMPO_TITULO),
                        LocalDate.parse(rs.getString(CAMPO_DATA_PUBLICACAO_DB)),
                        rs.getString("foto_url"),
                        rs.getString(CAMPO_CONTEUDO_HTML_DB)));
    }

    public BlogPostAdminForm carregarBlogPostFormulario(UUID id) {
        BlogPostAdminForm form = new BlogPostAdminForm();

        if (id == null) {
            return form;
        }

        BlogPostItem blogPost = buscarBlogPostPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(POSTAGEM_NAO_ENCONTRADA));

        form.setId(blogPost.getId());
        form.setTitulo(blogPost.getTitulo());
        form.setDataPublicacao(blogPost.getDataPublicacao() != null ? blogPost.getDataPublicacao().toString() : null);
        form.setFotoUrlAtual(blogPost.getFotoUrl());
        form.setConteudoHtml(blogPost.getConteudoHtml());

        return form;
    }

    @Transactional
    public void salvarBlogPost(String titulo, String dataPublicacao, MultipartFile foto, String conteudoHtml) {
        persistirNovoBlogPost(titulo, dataPublicacao, foto, conteudoHtml);
    }

    private void persistirNovoBlogPost(String titulo, String dataPublicacao, MultipartFile foto, String conteudoHtml) {
        ValidationUtils.validarCampoStringObrigatorio(titulo, CAMPO_TITULO);
        ValidationUtils.validarCampoStringObrigatorio(dataPublicacao, CAMPO_DATA_PUBLICACAO);
        ValidationUtils.validarCampoStringObrigatorio(conteudoHtml, CAMPO_CONTEUDO_HTML);

        String conteudoLimpado = conteudoHtml.trim();
        if (conteudoLimpado.replaceAll("<[^>]*>", "").isBlank()) {
            throw new IllegalArgumentException("Escreva o conteúdo da postagem.");
        }

        LocalDate data;
        try {
            data = LocalDate.parse(dataPublicacao.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Data de publicação inválida.");
        }

        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("Adicione uma foto para a postagem.");
        }

        String fotoUrl = fileUploadService.salvarImagem(foto);

        try {
            namedParameterJdbcTemplate.update(
                    "INSERT INTO blog_posts (id, titulo, data_publicacao, foto_url, conteudo_html) " +
                            "VALUES (:id, :titulo, :dataPublicacao, :fotoUrl, :conteudoHtml)",
                    new MapSqlParameterSource()
                            .addValue(CAMPO_ID, UUID.randomUUID().toString())
                            .addValue(CAMPO_TITULO, titulo.trim())
                            .addValue(CAMPO_DATA_PUBLICACAO, data.toString())
                            .addValue(CAMPO_FOTO_URL, fotoUrl)
                            .addValue(CAMPO_CONTEUDO_HTML, conteudoLimpado)
            );
        } catch (RuntimeException ex) {
            removerImagemIgnorandoFalhas(fotoUrl);
            throw ex;
        }
    }

    @Transactional
    public void salvarBlogPost(UUID id, String titulo, String dataPublicacao, MultipartFile foto, String conteudoHtml, String fotoUrlAtual) {
        if (id == null) {
            persistirNovoBlogPost(titulo, dataPublicacao, foto, conteudoHtml);
            return;
        }

        ValidationUtils.validarCampoStringObrigatorio(titulo, CAMPO_TITULO);
        ValidationUtils.validarCampoStringObrigatorio(dataPublicacao, CAMPO_DATA_PUBLICACAO);
        ValidationUtils.validarCampoStringObrigatorio(conteudoHtml, CAMPO_CONTEUDO_HTML);

        BlogPostItem existente = buscarBlogPostPorId(id)
            .orElseThrow(() -> new IllegalArgumentException(POSTAGEM_NAO_ENCONTRADA));

        String conteudoLimpado = conteudoHtml.trim();
        if (conteudoLimpado.replaceAll("<[^>]*>", "").isBlank()) {
            throw new IllegalArgumentException("Escreva o conteúdo da postagem.");
        }

        LocalDate data;
        try {
            data = LocalDate.parse(dataPublicacao.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Data de publicação inválida.");
        }

        String fotoUrl = existente.getFotoUrl();
        boolean novaFotoEnviada = foto != null && !foto.isEmpty();
        if (novaFotoEnviada) {
            fotoUrl = fileUploadService.salvarImagem(foto);
        }

        try {
            namedParameterJdbcTemplate.update(
                    "UPDATE blog_posts SET titulo = :titulo, data_publicacao = :dataPublicacao, foto_url = :fotoUrl, conteudo_html = :conteudoHtml WHERE id = :id",
                    new MapSqlParameterSource()
                            .addValue(CAMPO_ID, id.toString())
                            .addValue(CAMPO_TITULO, titulo.trim())
                            .addValue(CAMPO_DATA_PUBLICACAO, data.toString())
                            .addValue(CAMPO_FOTO_URL, fotoUrl)
                            .addValue(CAMPO_CONTEUDO_HTML, conteudoLimpado)
            );

            if (novaFotoEnviada && fotoUrlAtual != null && !fotoUrlAtual.isBlank() && !fotoUrlAtual.equals(fotoUrl)) {
                removerImagemIgnorandoFalhas(fotoUrlAtual);
            }
        } catch (RuntimeException ex) {
            if (novaFotoEnviada && fotoUrl != null && !fotoUrl.equals(existente.getFotoUrl())) {
                removerImagemIgnorandoFalhas(fotoUrl);
            }
            throw ex;
        }
    }

    @Transactional
    public void excluirBlogPost(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, CAMPO_ID);

        BlogPostItem existente = buscarBlogPostPorId(id)
            .orElseThrow(() -> new IllegalArgumentException(POSTAGEM_NAO_ENCONTRADA));

        namedParameterJdbcTemplate.update(
                "DELETE FROM blog_posts WHERE id = :id",
                new MapSqlParameterSource().addValue(CAMPO_ID, id.toString())
        );

        removerImagemIgnorandoFalhas(existente.getFotoUrl());
    }

    public String obterFotoAssociacaoUrl() {
        return buscarConfiguracao(CONFIG_FOTO_ASSOCIACAO).orElse(null);
    }

    @Transactional
    public void salvarFotoAssociacao(MultipartFile foto) {
        ValidationUtils.validarCampoObrigatorio(foto, "foto");

        String fotoAtual = buscarConfiguracao(CONFIG_FOTO_ASSOCIACAO).orElse(null);
        String novaFoto = fileUploadService.salvarImagem(foto);

        try {
            namedParameterJdbcTemplate.update(
                    "INSERT INTO configuracoes_site (chave, valor) VALUES (:chave, :valor) " +
                            "ON CONFLICT(chave) DO UPDATE SET valor = excluded.valor",
                    new MapSqlParameterSource()
                            .addValue(CAMPO_CHAVE, CONFIG_FOTO_ASSOCIACAO)
                            .addValue(CAMPO_VALOR, novaFoto)
            );

            if (fotoAtual != null && !fotoAtual.isBlank() && !fotoAtual.equals(novaFoto)) {
                removerImagemIgnorandoFalhas(fotoAtual);
            }
        } catch (RuntimeException ex) {
            removerImagemIgnorandoFalhas(novaFoto);
            throw ex;
        }
    }

    @Transactional
    public void salvarArtesao(ArtesaoAdminForm form, MultipartFile foto) {
        ValidationUtils.validarCampoObrigatorio(form, "formulario");
        ValidationUtils.validarCampoStringObrigatorio(form.getNome(), "nome");
        ValidationUtils.validarCampoStringObrigatorio(form.getUsername(), "username");

        if (form.getId() == null) {
            ValidationUtils.validarCampoStringObrigatorio(form.getPassword(), "password");
            criarArtesao(form, foto);
            return;
        }

        atualizarArtesao(form, foto);
    }

    @Transactional
    public void salvarGaleria(String titulo, MultipartFile[] fotos) {
        ValidationUtils.validarCampoStringObrigatorio(titulo, CAMPO_TITULO);

        List<MultipartFile> fotosValidas = fotos == null ? List.of() : Arrays.stream(fotos)
                .filter(foto -> foto != null && !foto.isEmpty())
                .toList();

        if (fotosValidas.isEmpty()) {
            throw new IllegalArgumentException("Adicione ao menos uma foto para a galeria.");
        }

        UUID galeriaId = UUID.randomUUID();
        List<String> arquivosSalvos = new ArrayList<>();

        try {
            namedParameterJdbcTemplate.update(
                    "INSERT INTO galerias (id, titulo) VALUES (:id, :titulo)",
                    new MapSqlParameterSource()
                        .addValue(CAMPO_ID, galeriaId.toString())
                        .addValue(CAMPO_TITULO, titulo.trim())
            );

            for (int index = 0; index < fotosValidas.size(); index++) {
                MultipartFile foto = fotosValidas.get(index);
                String arquivoUrl = fileUploadService.salvarImagem(foto);
                arquivosSalvos.add(arquivoUrl);

                namedParameterJdbcTemplate.update(
                        "INSERT INTO galeria_fotos (id, galeria_id, arquivo_url, ordem) VALUES (:id, :galeriaId, :arquivoUrl, :ordem)",
                        new MapSqlParameterSource()
                        .addValue(CAMPO_ID, UUID.randomUUID().toString())
                                .addValue("galeriaId", galeriaId.toString())
                                .addValue("arquivoUrl", arquivoUrl)
                                .addValue("ordem", index)
                );
            }
        } catch (RuntimeException ex) {
            for (String arquivoUrl : arquivosSalvos) {
                try {
                    fileUploadService.removerImagem(arquivoUrl);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
            namedParameterJdbcTemplate.update(
                    "DELETE FROM galerias WHERE id = :id",
                    new MapSqlParameterSource().addValue(CAMPO_ID, galeriaId.toString())
            );
            throw ex;
        }
    }

    @Transactional
    public void excluirArtesao(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");

        Artesao artesao = artesaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(ARTESAO_NAO_ENCONTRADO));

        if (artesao.getFotoUrl() != null && !artesao.getFotoUrl().isBlank()) {
            try {
                fileUploadService.removerImagem(artesao.getFotoUrl());
            } catch (Exception ignored) {
                // ignore cleanup failures
            }
        }

        // remove dependent rows directly to avoid Spring Data JDBC FOR UPDATE behavior on SQLite
        namedParameterJdbcTemplate.update(
            "DELETE FROM movimentacoes WHERE artesao_id = :artesaoId",
            new MapSqlParameterSource().addValue(CAMPO_ARTESAO_ID, requireId(artesao.getId(), ARTESAO_NAO_ENCONTRADO)));

        // remove product images first, then the rows
        List<String> imagensProdutos = namedParameterJdbcTemplate.query(
                "SELECT imagem_url FROM produtos WHERE artesao_id = :artesaoId",
            new MapSqlParameterSource().addValue(CAMPO_ARTESAO_ID, requireId(artesao.getId(), ARTESAO_NAO_ENCONTRADO)),
                (rs, rowNum) -> rs.getString("imagem_url")
        );
        for (String imagemUrl : imagensProdutos) {
            if (imagemUrl != null && !imagemUrl.isBlank()) {
                try {
                    fileUploadService.removerImagem(imagemUrl);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
        }
        namedParameterJdbcTemplate.update(
                "DELETE FROM produtos WHERE artesao_id = :artesaoId",
            new MapSqlParameterSource().addValue(CAMPO_ARTESAO_ID, requireId(artesao.getId(), ARTESAO_NAO_ENCONTRADO)));

        if (artesao.getUsuarioId() != null) {
            namedParameterJdbcTemplate.update(
                    "DELETE FROM usuarios WHERE id = :usuarioId",
                    new MapSqlParameterSource().addValue(CAMPO_USUARIO_ID, requireId(artesao.getUsuarioId(), ARTESAO_SEM_USUARIO_VINCULADO)));
        }

        namedParameterJdbcTemplate.update(
                "DELETE FROM artesaos WHERE id = :id",
            new MapSqlParameterSource().addValue(CAMPO_ID, requireId(artesao.getId(), ARTESAO_NAO_ENCONTRADO)));
    }

    private void criarArtesao(ArtesaoAdminForm form, MultipartFile foto) {
        String username = form.getUsername().trim();
        String password = form.getPassword().trim();

        if (usuarioService.buscarPorUsername(username).isPresent()) {
            throw new IllegalArgumentException("Usuario ja existe: " + username);
        }

        String fotoUrl = salvarFotoSeExistir(foto);

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRole(Role.ROLE_ARTESAO);
        usuario.setNew(true);

        Artesao artesao = new Artesao(
                UUID.randomUUID(),
                form.getNome().trim(),
                limparOuNulo(form.getDescricao()),
                limparOuNulo(form.getWhatsapp()),
                fotoUrl,
                usuario.getId(),
                new LinkedHashSet<>(),
                true);

        usuarioService.salvarNovoUsuario(usuario);
        namedParameterJdbcTemplate.update(
            "INSERT INTO artesaos (id, nome, descricao, whatsapp, foto_url, usuario_id) " +
            "VALUES (:id, :nome, :descricao, :whatsapp, :fotoUrl, :usuarioId)",
            new MapSqlParameterSource()
                .addValue(CAMPO_ID, requireId(artesao.getId(), ARTESAO_NAO_ENCONTRADO))
                .addValue("nome", artesao.getNome())
                .addValue("descricao", artesao.getDescricao())
                .addValue("whatsapp", artesao.getWhatsapp())
                .addValue(CAMPO_FOTO_URL, artesao.getFotoUrl())
                .addValue(CAMPO_USUARIO_ID, requireId(artesao.getUsuarioId(), ARTESAO_SEM_USUARIO_VINCULADO))
        );
    }

    private void atualizarArtesao(ArtesaoAdminForm form, MultipartFile foto) {
        Artesao artesao = artesaoRepository.findById(form.getId())
            .orElseThrow(() -> new IllegalArgumentException(ARTESAO_NAO_ENCONTRADO));

        UUID usuarioId = artesao.getUsuarioId();
        if (usuarioId == null) {
            throw new IllegalArgumentException(ARTESAO_SEM_USUARIO_VINCULADO);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario vinculado nao encontrado."));

        String novoUsername = form.getUsername().trim();
        if (!novoUsername.equals(usuario.getUsername())) {
            usuarioService.buscarPorUsername(novoUsername)
                    .filter(existing -> !existing.getId().equals(usuario.getId()))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Usuario ja existe: " + novoUsername);
                    });
            usuario.setUsername(novoUsername);
        }

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(form.getPassword().trim()));
        }

        usuario.setRole(Role.ROLE_ARTESAO);

        if (artesao.getNome() != null || form.getNome() != null) {
            artesao.setNome(form.getNome().trim());
        }
        artesao.setDescricao(limparOuNulo(form.getDescricao()));
        artesao.setWhatsapp(limparOuNulo(form.getWhatsapp()));

        if (foto != null && !foto.isEmpty()) {
            if (artesao.getFotoUrl() != null && !artesao.getFotoUrl().isBlank()) {
                try {
                    fileUploadService.removerImagem(artesao.getFotoUrl());
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
            artesao.setFotoUrl(salvarFotoSeExistir(foto));
        }

        usuarioRepository.save(usuario);
        namedParameterJdbcTemplate.update(
            "UPDATE artesaos SET nome = :nome, descricao = :descricao, whatsapp = :whatsapp, " +
            "foto_url = :fotoUrl, usuario_id = :usuarioId WHERE id = :id",
            new MapSqlParameterSource()
                .addValue(CAMPO_ID, requireId(artesao.getId(), ARTESAO_NAO_ENCONTRADO))
                .addValue("nome", artesao.getNome())
                .addValue("descricao", artesao.getDescricao())
                .addValue("whatsapp", artesao.getWhatsapp())
                .addValue(CAMPO_FOTO_URL, artesao.getFotoUrl())
                .addValue(CAMPO_USUARIO_ID, requireId(artesao.getUsuarioId(), ARTESAO_SEM_USUARIO_VINCULADO))
        );
    }

    private String salvarFotoSeExistir(MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            return null;
        }
        return fileUploadService.salvarImagem(foto);
    }

    private ArtesaoDashboardItem toDashboardItem(Artesao artesao, Map<UUID, Integer> totalProdutosPorArtesao) {
        String username = "(sem login)";
        if (artesao.getUsuarioId() != null) {
            username = usuarioRepository.findById(artesao.getUsuarioId())
                    .map(Usuario::getUsername)
                    .orElse(username);
        }

        return new ArtesaoDashboardItem(
                artesao.getId(),
                artesao.getNome(),
                artesao.getDescricao(),
                artesao.getWhatsapp(),
                artesao.getFotoUrl(),
                username,
                totalProdutosPorArtesao.getOrDefault(artesao.getId(), 0));
    }

    private Map<UUID, Integer> contarProdutosPorArtesao() {
        Map<UUID, Integer> totalProdutosPorArtesao = new HashMap<>();
        StreamSupport.stream(produtoRepository.findAll().spliterator(), false)
                .forEach(produto -> {
                    if (produto.getArtesaoId() == null) {
                        return;
                    }
                    totalProdutosPorArtesao.merge(produto.getArtesaoId(), 1, Integer::sum);
                });
        return totalProdutosPorArtesao;
    }

    private int quantidadeVendida(Produto produto) {
        return Optional.ofNullable(produto.getQuantidadeVendida()).orElse(0);
    }

    private String requireId(UUID id, String message) {
        return Objects.requireNonNull(id, message).toString();
    }

    private String limparOuNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpado = valor.trim();
        return limpado.isEmpty() ? null : limpado;
    }

    private void removerImagemIgnorandoFalhas(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return;
        }

        try {
            fileUploadService.removerImagem(nomeArquivo);
        } catch (Exception ignored) {
            // ignore cleanup failures
        }
    }

    private Optional<BlogPostItem> buscarBlogPostPorId(UUID id) {
        List<BlogPostItem> posts = namedParameterJdbcTemplate.query(
                "SELECT id, titulo, data_publicacao, foto_url, conteudo_html FROM blog_posts WHERE id = :id",
                new MapSqlParameterSource().addValue(CAMPO_ID, id.toString()),
                (rs, rowNum) -> new BlogPostItem(
                        UUID.fromString(rs.getString(CAMPO_ID)),
                        rs.getString(CAMPO_TITULO),
                        LocalDate.parse(rs.getString(CAMPO_DATA_PUBLICACAO_DB)),
                        rs.getString("foto_url"),
                        rs.getString(CAMPO_CONTEUDO_HTML_DB)));

        if (posts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(posts.get(0));
    }

    private Optional<String> buscarConfiguracao(String chave) {
        List<String> valores = namedParameterJdbcTemplate.query(
                "SELECT valor FROM configuracoes_site WHERE chave = :chave",
                new MapSqlParameterSource().addValue(CAMPO_CHAVE, chave),
                (rs, rowNum) -> rs.getString(CAMPO_VALOR));
        if (valores.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(valores.get(0));
    }
}
