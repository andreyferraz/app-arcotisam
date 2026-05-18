package com.arcotisam.app.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.arcotisam.app.dto.ProdutoRankingItem;
import com.arcotisam.app.enuns.Role;
import com.arcotisam.app.model.Artesao;
import com.arcotisam.app.model.Produto;
import com.arcotisam.app.model.Usuario;
import com.arcotisam.app.repository.ArtesaoRepository;
import com.arcotisam.app.repository.MovimentacaoRepository;
import com.arcotisam.app.repository.ProdutoRepository;
import com.arcotisam.app.repository.UsuarioRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class AdminMasterService {

    private static final String ARTESAO_NAO_ENCONTRADO = "Artesao nao encontrado.";

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
                new MapSqlParameterSource().addValue("artesaoId", artesao.getId().toString()));

        // remove product images first, then the rows
        List<String> imagensProdutos = namedParameterJdbcTemplate.query(
                "SELECT imagem_url FROM produtos WHERE artesao_id = :artesaoId",
                new MapSqlParameterSource().addValue("artesaoId", artesao.getId().toString()),
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
                new MapSqlParameterSource().addValue("artesaoId", artesao.getId().toString()));

        if (artesao.getUsuarioId() != null) {
            namedParameterJdbcTemplate.update(
                    "DELETE FROM usuarios WHERE id = :usuarioId",
                    new MapSqlParameterSource().addValue("usuarioId", artesao.getUsuarioId().toString()));
        }

        namedParameterJdbcTemplate.update(
                "DELETE FROM artesaos WHERE id = :id",
                new MapSqlParameterSource().addValue("id", artesao.getId().toString()));
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
                .addValue("id", artesao.getId().toString())
                .addValue("nome", artesao.getNome())
                .addValue("descricao", artesao.getDescricao())
                .addValue("whatsapp", artesao.getWhatsapp())
                .addValue("fotoUrl", artesao.getFotoUrl())
                .addValue("usuarioId", artesao.getUsuarioId().toString())
        );
    }

    private void atualizarArtesao(ArtesaoAdminForm form, MultipartFile foto) {
        Artesao artesao = artesaoRepository.findById(form.getId())
            .orElseThrow(() -> new IllegalArgumentException(ARTESAO_NAO_ENCONTRADO));

        UUID usuarioId = artesao.getUsuarioId();
        if (usuarioId == null) {
            throw new IllegalArgumentException("Artesao sem usuario vinculado.");
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
                .addValue("id", artesao.getId().toString())
                .addValue("nome", artesao.getNome())
                .addValue("descricao", artesao.getDescricao())
                .addValue("whatsapp", artesao.getWhatsapp())
                .addValue("fotoUrl", artesao.getFotoUrl())
                .addValue("usuarioId", artesao.getUsuarioId().toString())
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

    private String limparOuNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpado = valor.trim();
        return limpado.isEmpty() ? null : limpado;
    }
}
