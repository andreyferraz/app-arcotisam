package com.arcotisam.app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        return "index";
    }

    @GetMapping("/sobre")
    public String sobre(Model model) {
        return "sobre";
    }

    @GetMapping("/loja")
    public String loja(Model model) {
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

    @GetMapping("/carrinho")
    public String carrinho(Model model) {
        return "carrinho";
    }
}
