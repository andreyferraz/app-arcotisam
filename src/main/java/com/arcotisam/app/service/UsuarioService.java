package com.arcotisam.app.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.arcotisam.app.model.Usuario;
import com.arcotisam.app.enuns.Role;
import com.arcotisam.app.repository.UsuarioRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final String USERNAME_FIELD = "username";
    private static final String PASSWORD_FIELD = "password";

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, FileUploadService fileUploadService, NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Optional<Usuario> buscarPorId(UUID id){
        ValidationUtils.validarCampoObrigatorio(id, "id");
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username){
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);

        var params = new MapSqlParameterSource().addValue(USERNAME_FIELD, username);
        List<Usuario> usuarios = namedParameterJdbcTemplate.query(
                "SELECT id, username, password, role, foto_url FROM usuarios WHERE username = :username",
                params,
                this::mapUsuario);
        return usuarios.stream().findFirst();
    }

    @Transactional
    public Usuario criarUsuario(String username, String password) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, PASSWORD_FIELD);

        if (buscarPorUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username já existe" + username);
        }

        String hash = passwordEncoder.encode(password);

        Usuario novo = new Usuario();
        novo.setId(UUID.randomUUID());
        novo.setUsername(username);
        novo.setPassword(hash);
        novo.setRole(Role.ROLE_ARTESAO);
        novo.setNew(true);
        return salvarNovoUsuario(novo);

    }

    @Transactional
    public Usuario criarAdmin(String username, String password, MultipartFile foto) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, PASSWORD_FIELD);

        buscarPorUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Usuario já existe: " + username);
        });

        String hash = passwordEncoder.encode(password);

        Usuario novo = new Usuario();
        novo.setId(UUID.randomUUID());
        novo.setUsername(username);
        novo.setPassword(hash);
        novo.setRole(Role.ROLE_ADMIN_MASTER);
        novo.setNew(true);

        if (foto != null && !foto.isEmpty()) {
            String savedName = fileUploadService.salvarImagem(foto);
            novo.setFotoUrl(savedName);
        }

        return salvarNovoUsuario(novo);
    }

    public boolean autenticar(String username, String password) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, PASSWORD_FIELD);

        Optional<Usuario> opt = buscarPorUsername(username);
        if (opt.isEmpty()) {
            return false;
        }

        String stored = opt.get().getPassword();
        return passwordEncoder.matches(password, stored);
    }

    @Transactional
    public Usuario atualizarSenha(UUID id, String novaSenha) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        ValidationUtils.validarCampoStringObrigatorio(novaSenha, "novaSenha");

        Usuario existente = usuarioRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado."));

        String hash = passwordEncoder.encode(novaSenha);

        var params = new MapSqlParameterSource()
            .addValue("id", id.toString())
            .addValue(PASSWORD_FIELD, hash);

        namedParameterJdbcTemplate.update(
            "UPDATE usuarios SET password = :password WHERE id = :id",
            params);

        // reload and return using explicit query to avoid possible repository mapping issues
        var out = namedParameterJdbcTemplate.query(
            "SELECT id, username, password, role, foto_url FROM usuarios WHERE id = :id",
            new MapSqlParameterSource().addValue("id", id.toString()),
            this::mapUsuario);

        return out.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado apos update."));
    }

    @Transactional
    public void deletar(UUID id) {
        ValidationUtils.validarCampoObrigatorio(id, "id");
        var opt = usuarioRepository.findById(id);
        if (opt.isPresent()) {
            var u = opt.get();
            if (u.getFotoUrl() != null && !u.getFotoUrl().isEmpty()) {
                try { fileUploadService.removerImagem(u.getFotoUrl()); } catch (Exception e) { /* ignore */ }
            }
        }
        usuarioRepository.deleteById(id);
    }

    public Usuario salvarNovoUsuario(Usuario novo) {
        ValidationUtils.validarCampoObrigatorio(novo, "usuario");
        ValidationUtils.validarCampoObrigatorio(novo.getId(), "id");
        ValidationUtils.validarCampoStringObrigatorio(novo.getUsername(), USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(novo.getPassword(), PASSWORD_FIELD);
        ValidationUtils.validarCampoObrigatorio(novo.getRole(), "role");

        UUID id = Objects.requireNonNull(novo.getId(), "id");
        String username = novo.getUsername();
        String password = novo.getPassword();
        String fotoUrl = novo.getFotoUrl();

        if (buscarPorUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username já existe" + username);
        }

        var params = new MapSqlParameterSource()
                .addValue("id", id.toString())
                .addValue(USERNAME_FIELD, username)
                .addValue(PASSWORD_FIELD, password)
                .addValue("role", novo.getRole().name())
                .addValue("fotoUrl", fotoUrl);

        namedParameterJdbcTemplate.update(
                "INSERT INTO usuarios (id, username, password, role, foto_url) VALUES (:id, :username, :password, :role, :fotoUrl)",
                params);

        novo.setNew(false);
        return novo;
    }

    private Usuario mapUsuario(ResultSet rs, int rowNum) throws SQLException {
        Usuario usuario = new Usuario();
        String id = rs.getString("id");
        if (id != null) {
            usuario.setId(UUID.fromString(id));
        }
        usuario.setUsername(rs.getString(USERNAME_FIELD));
        usuario.setPassword(rs.getString(PASSWORD_FIELD));
        usuario.setFotoUrl(rs.getString("foto_url"));

        String role = rs.getString("role");
        if (role != null && !role.isBlank()) {
            usuario.setRole(Role.valueOf(role));
        }

        usuario.setNew(false);
        return usuario;
    }


}
