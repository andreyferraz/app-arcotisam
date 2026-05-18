package com.arcotisam.app.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.arcotisam.app.model.Artesao;
import com.arcotisam.app.model.Movimentacao;
import com.arcotisam.app.model.Produto;
import com.arcotisam.app.service.ArtesaoService;
import com.arcotisam.app.service.MovimentacaoService;
import com.arcotisam.app.service.ProdutoService;
import com.arcotisam.app.service.UsuarioService;

@Controller
@RequestMapping("/artesao")
public class ArtesaoController {

    private final UsuarioService usuarioService;
    private final ArtesaoService artesaoService;
    private final ProdutoService produtoService;
    private final MovimentacaoService movimentacaoService;

    public ArtesaoController(UsuarioService usuarioService, ArtesaoService artesaoService, ProdutoService produtoService, MovimentacaoService movimentacaoService) {
        this.usuarioService = usuarioService;
        this.artesaoService = artesaoService;
        this.produtoService = produtoService;
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping
    public String painel(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null) return "redirect:/login";

        var optUser = usuarioService.buscarPorUsername(username);
        if (optUser.isEmpty()) return "redirect:/login";

        var user = optUser.get();
        var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
        Artesao artesao = null;
        if (optArtesao.isPresent()) {
            artesao = optArtesao.get();
        }

        if (artesao == null) {
            model.addAttribute("mensagem", "Perfil de artesão não encontrado. Por favor cadastre-se como artesão.");
            return "artesao";
        }

        

        List<Produto> produtos = produtoService.listarPorArtesao(artesao.getId());
        List<Movimentacao> movimentacoes = movimentacaoService.listarPorArtesao(artesao.getId());

        model.addAttribute("artesao", artesao);
        model.addAttribute("produtos", produtos);
        model.addAttribute("movimentacoes", movimentacoes);

        return "artesao";
    }

    @PostMapping("/produtos/salvar")
    public String salvarProduto(@RequestParam String nome,
                                @RequestParam(required = false) String descricao,
                                @RequestParam(required = false) BigDecimal preco,
                                @RequestParam(required = false) MultipartFile imagem,
                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null) return "redirect:/login";

        var optUser = usuarioService.buscarPorUsername(username);
        if (optUser.isEmpty()) return "redirect:/login";

        var user = optUser.get();
        var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
        if (optArtesao.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Perfil de artesão não encontrado.");
            return "redirect:/artesao";
        }

        var artesao = optArtesao.get();
        produtoService.criarProduto(nome, descricao, preco, artesao.getId(), imagem);
        redirectAttributes.addFlashAttribute("sucesso", "Produto cadastrado com sucesso.");
        return "redirect:/artesao";
    }

    @PostMapping("/produtos/{id}/excluir")
    public String excluirProduto(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            produtoService.deletar(id);
            redirectAttributes.addFlashAttribute("sucesso", "Produto excluído com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/artesao";
    }

    @PostMapping("/produtos/{id}/atualizar")
    public String atualizarProduto(@PathVariable UUID id,
                                   @RequestParam String nome,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam(required = false) java.math.BigDecimal preco,
                                   @RequestParam(required = false) MultipartFile imagem,
                                   RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null) return "redirect:/login";

            var optUser = usuarioService.buscarPorUsername(username);
            if (optUser.isEmpty()) return "redirect:/login";

            var user = optUser.get();
            var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
            if (optArtesao.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Perfil de artesão não encontrado.");
                return "redirect:/artesao";
            }

            var artesao = optArtesao.get();

            // ownership check
            var optProd = produtoService.buscarPorId(id);
            if (optProd.isEmpty() || !artesao.getId().equals(optProd.get().getArtesaoId())) {
                redirectAttributes.addFlashAttribute("erro", "Produto não encontrado ou sem permissão para editar.");
                return "redirect:/artesao";
            }

            produtoService.atualizar(id, nome, descricao, preco, imagem);
            redirectAttributes.addFlashAttribute("sucesso", "Produto atualizado com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/artesao";
    }

    @PostMapping("/lancamentos/salvar")
    public String salvarLancamento(@RequestParam String tipo,
                                   @RequestParam(required = false) String descricao,
                                   @RequestParam java.math.BigDecimal valor,
                                   RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        if (username == null) return "redirect:/login";

        var optUser = usuarioService.buscarPorUsername(username);
        if (optUser.isEmpty()) return "redirect:/login";

        var user = optUser.get();
        var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
        if (optArtesao.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Perfil de artesão não encontrado.");
            return "redirect:/artesao";
        }

        var artesao = optArtesao.get();
        movimentacaoService.lancar(artesao.getId(), tipo, descricao, valor);
        redirectAttributes.addFlashAttribute("sucesso", "Lançamento registrado.");
        return "redirect:/artesao";
    }

    @PostMapping("/senha/alterar")
    public String alterarSenha(@RequestParam(name = "novaSenha", required = true) String novaSenha,
                               RedirectAttributes redirectAttributes) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null) {
                redirectAttributes.addFlashAttribute("erro", "Não foi possível identificar o usuário autenticado.");
                return "redirect:/artesao";
            }

            var opt = usuarioService.buscarPorUsername(username);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Usuário autenticado não encontrado.");
                return "redirect:/artesao";
            }

            usuarioService.atualizarSenha(opt.get().getId(), novaSenha);
            redirectAttributes.addFlashAttribute("sucesso", "Senha atualizada com sucesso.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }

        return "redirect:/artesao";
    }

}
