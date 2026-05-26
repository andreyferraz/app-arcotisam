package com.arcotisam.app.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.arcotisam.app.model.Artesao;
import com.arcotisam.app.model.Produto;
import com.arcotisam.app.repository.ArtesaoRepository;
import com.arcotisam.app.service.ProdutoService;

@Controller
public class PageController {

    private final ProdutoService produtoService;
    private final ArtesaoRepository artesaoRepository;

    public PageController(ProdutoService produtoService, ArtesaoRepository artesaoRepository) {
        this.produtoService = produtoService;
        this.artesaoRepository = artesaoRepository;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("ultimosProdutos", produtoService.listarUltimosCadastrados(3));
        return "index";
    }

    @GetMapping("/sobre")
    public String sobre(Model model) {
        return "sobre";
    }

    @GetMapping("/loja")
    public String loja(@RequestParam(value = "artesaoId", required = false) UUID artesaoId, Model model) {
        List<Produto> produtos;
        if (artesaoId != null) {
            produtos = produtoService.listarPorArtesao(artesaoId);
        } else {
            produtos = produtoService.listarTodos();
        }

        List<Artesao> artesaos = StreamSupport.stream(artesaoRepository.findAll().spliterator(), false)
            .toList();

        model.addAttribute("produtos", produtos);
        model.addAttribute("artesaos", artesaos);
        model.addAttribute("selectedArtesaoId", artesaoId != null ? artesaoId.toString() : "");

        return "loja";
    }

    @GetMapping("/associados")
    public String associados(Model model) {
        return "associados";
    }

    @GetMapping("/contato")
    public String contato(Model model) {
        return "contato";
    }

    @GetMapping("/comprar/{produtoId}")
    public String comprar(@PathVariable UUID produtoId, RedirectAttributes redirectAttributes) {
        try {
            String linkWhatsApp = produtoService.registrarCliqueEObterLinkWhatsApp(produtoId);
            return "redirect:" + linkWhatsApp;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/loja";
        }
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }
}
