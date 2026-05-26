package com.arcotisam.app.controller;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
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
import com.arcotisam.app.service.AdminMasterService;
import com.arcotisam.app.service.ProdutoService;

@Controller
public class PageController {

    private final ProdutoService produtoService;
    private final ArtesaoRepository artesaoRepository;
    private final AdminMasterService adminMasterService;

    public PageController(ProdutoService produtoService, ArtesaoRepository artesaoRepository, AdminMasterService adminMasterService) {
        this.produtoService = produtoService;
        this.artesaoRepository = artesaoRepository;
        this.adminMasterService = adminMasterService;
    }

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("ultimosProdutos", produtoService.listarUltimosCadastrados(3));
        model.addAttribute("artesaosAleatorios", listarArtesaosAleatorios(3));
        model.addAttribute("fotoAssociacaoUrl", resolverFotoAssociacaoUrl());
        return "index";
    }

    @GetMapping("/sobre")
    public String sobre(Model model) {
        model.addAttribute("galerias", adminMasterService.listarGalerias());
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

    private List<Artesao> listarArtesaosAleatorios(int limite) {
        List<Artesao> artesaos = StreamSupport.stream(artesaoRepository.findAll().spliterator(), false)
            .filter(artesao -> artesao.getId() != null)
            .toList();

        if (artesaos.isEmpty()) {
            return artesaos;
        }

        List<Artesao> embaralhados = new ArrayList<>(artesaos);
        Collections.shuffle(embaralhados, ThreadLocalRandom.current());
        return embaralhados.subList(0, Math.min(limite, embaralhados.size()));
    }

    private String resolverFotoAssociacaoUrl() {
        String fotoAssociacao = adminMasterService.obterFotoAssociacaoUrl();
        if (fotoAssociacao == null || fotoAssociacao.isBlank()) {
            return "/img/foto_associacao.webp";
        }
        return "/uploads/" + fotoAssociacao;
    }
}
