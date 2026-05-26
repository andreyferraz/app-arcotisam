package com.arcotisam.app.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.arcotisam.app.dto.ArtesaoAdminForm;
import com.arcotisam.app.dto.ArtesaoDashboardItem;
import com.arcotisam.app.dto.GaleriaExibicaoItem;
import com.arcotisam.app.dto.ProdutoRankingItem;
import com.arcotisam.app.service.AdminMasterService;
import com.arcotisam.app.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String ADMIN_REDIRECT = "redirect:/admin";

    private final AdminMasterService adminMasterService;
    private final UsuarioService usuarioService;
    private static final String SUCESSO = "sucesso";

    public AdminController(AdminMasterService adminMasterService, UsuarioService usuarioService) {
        this.adminMasterService = adminMasterService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String painel(@RequestParam(name = "editar", required = false) UUID editar, Model model) {
        ArtesaoAdminForm form = adminMasterService.carregarFormulario(editar);
        List<ArtesaoDashboardItem> artesaos = adminMasterService.listarArtesaos();
        List<ProdutoRankingItem> produtosMaisVendidos = adminMasterService.listarProdutosMaisVendidos(5);
        List<GaleriaExibicaoItem> galerias = adminMasterService.listarGalerias();

        model.addAttribute("artesaoForm", form);
        model.addAttribute("artesaos", artesaos);
        model.addAttribute("produtosMaisVendidos", produtosMaisVendidos);
        model.addAttribute("galerias", galerias);
        model.addAttribute("labelsProdutosMaisVendidos", produtosMaisVendidos.stream().map(ProdutoRankingItem::getNome).toList());
        model.addAttribute("valoresProdutosMaisVendidos", produtosMaisVendidos.stream().map(ProdutoRankingItem::getQuantidadeVendida).toList());
        model.addAttribute("maxQuantidadeVendida", produtosMaisVendidos.stream().mapToInt(item -> item.getQuantidadeVendida() == null ? 0 : item.getQuantidadeVendida()).max().orElse(0));
        model.addAttribute("totalArtesaos", adminMasterService.contarArtesaos());
        model.addAttribute("totalProdutos", adminMasterService.contarProdutos());
        model.addAttribute("totalVendas", adminMasterService.contarVendas());
        model.addAttribute("edicao", editar != null);

        return "admin";
    }

    @PostMapping("/artesaos/salvar")
    public String salvarArtesao(
            @ModelAttribute("artesaoForm") ArtesaoAdminForm form,
            @RequestParam(name = "foto", required = false) MultipartFile foto,
            RedirectAttributes redirectAttributes) {
        try {
            boolean edicao = form.getId() != null;
            adminMasterService.salvarArtesao(form, foto);
            redirectAttributes.addFlashAttribute(SUCESSO, edicao ? "Artesão atualizado com sucesso." : "Artesão cadastrado com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            if (form.getId() != null) {
                return "redirect:/admin?editar=" + form.getId();
            }
            return ADMIN_REDIRECT;
        }

        return ADMIN_REDIRECT;
    }

    @PostMapping("/artesaos/{id}/excluir")
    public String excluirArtesao(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            adminMasterService.excluirArtesao(id);
            redirectAttributes.addFlashAttribute(SUCESSO, "Artesão excluído com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }

        return ADMIN_REDIRECT;
    }

    @PostMapping("/galerias/salvar")
    public String salvarGaleria(@RequestParam(name = "titulo") String titulo,
                                @RequestParam(name = "fotos", required = false) MultipartFile[] fotos,
                                RedirectAttributes redirectAttributes) {
        try {
            adminMasterService.salvarGaleria(titulo, fotos);
            redirectAttributes.addFlashAttribute(SUCESSO, "Galeria cadastrada com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }

        return ADMIN_REDIRECT;
    }

    @PostMapping("/senha/alterar")
    public String alterarSenha(@RequestParam(name = "novaSenha", required = true) String novaSenha,
                               RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null) {
                redirectAttributes.addFlashAttribute("erro", "Não foi possível identificar o usuário autenticado.");
                return ADMIN_REDIRECT;
            }

            var opt = usuarioService.buscarPorUsername(username);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Usuário autenticado não encontrado.");
                return ADMIN_REDIRECT;
            }

            usuarioService.atualizarSenha(opt.get().getId(), novaSenha);
            redirectAttributes.addFlashAttribute(SUCESSO, "Senha atualizada com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }

        return adminRedirect();
    }

    private String adminRedirect() {
        return ADMIN_REDIRECT;
    }
}
