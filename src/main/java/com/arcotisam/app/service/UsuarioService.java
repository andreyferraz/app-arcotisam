package com.arcotisam.app.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.arcotisam.app.model.Usuario;
import com.arcotisam.app.repository.UsuarioRepository;
import com.arcotisam.app.utils.ValidationUtils;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileUploadService fileUploadService;
     private static final String USERNAME_FIELD = "username";

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, FileUploadService fileUploadService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileUploadService = fileUploadService;
    }

    public Optional<Usuario> buscarPorId(UUID id){
        ValidationUtils.validarCampoObrigatorio(id, "id");
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorUsername(String username){
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        return usuarioRepository.findByUsername(username);
    }

    @Transactional
    public Usuario criarUsuario(String username, String password) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, "password");

        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username já existe" + username);
        }

        String hash = passwordEncoder.encode(password);

        Usuario novo = new Usuario(UUID.randomUUID(), username, hash, null, null, true);
        return usuarioRepository.save(novo);

    }

    @Transactional
    public Usuario criarAdmin(String username, String password, MultipartFile foto) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, "password");

        usuarioRepository.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Usuario já existe: " + username);
        });

        String hash = passwordEncoder.encode(password);

        Usuario novo = new Usuario(UUID.randomUUID(), username, hash, null, null, true);

        if (foto != null && !foto.isEmpty()) {
            String savedName = fileUploadService.salvarImagem(foto);
            novo.setFotoUrl(savedName);
        }

        return usuarioRepository.save(novo);
    }

    public boolean autenticar(String username, String password) {
        ValidationUtils.validarCampoStringObrigatorio(username, USERNAME_FIELD);
        ValidationUtils.validarCampoStringObrigatorio(password, "password");

        Optional<Usuario> opt = usuarioRepository.findByUsername(username);
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

        existente.setPassword(passwordEncoder.encode(novaSenha));
        return usuarioRepository.save(existente);
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


}
