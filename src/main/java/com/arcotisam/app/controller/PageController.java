package com.arcotisam.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("title", "Home - ARCOTISAM");
        model.addAttribute("view", "home");
        return "layout";
    }

    @GetMapping("/sobre")
    public String sobre(Model model) {
        model.addAttribute("title", "Sobre - ARCOTISAM");
        model.addAttribute("view", "sobre");
        return "layout";
    }

    @GetMapping("/loja")
    public String loja(Model model) {
        model.addAttribute("title", "Loja - ARCOTISAM");
        model.addAttribute("view", "loja");
        return "layout";
    }

    @GetMapping("/associados")
    public String associados(Model model) {
        model.addAttribute("title", "Associados - ARCOTISAM");
        model.addAttribute("view", "associados");
        return "layout";
    }

    @GetMapping("/contato")
    public String contato(Model model) {
        model.addAttribute("title", "Contato - ARCOTISAM");
        model.addAttribute("view", "contato");
        return "layout";
    }

    @GetMapping("/carrinho")
    public String carrinho(Model model) {
        model.addAttribute("title", "Carrinho - ARCOTISAM");
        model.addAttribute("view", "carrinho");
        return "layout";
    }
}
