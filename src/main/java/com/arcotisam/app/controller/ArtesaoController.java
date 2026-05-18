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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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

    @GetMapping("/relatorios/dia")
    public ResponseEntity<ByteArrayResource> relatorioDia(@RequestParam(name = "data") String dataStr) {
        // dataStr expected YYYY-MM-DD
        try {
            java.time.LocalDate data = java.time.LocalDate.parse(dataStr);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null) return ResponseEntity.status(401).build();

            var optUser = usuarioService.buscarPorUsername(username);
            if (optUser.isEmpty()) return ResponseEntity.status(401).build();

            var user = optUser.get();
            var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
            if (optArtesao.isEmpty()) return ResponseEntity.badRequest().build();
            var artesao = optArtesao.get();

            var inicio = data.atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            var fim = data.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);

            var list = movimentacaoService.listarPorPeriodo(artesao.getId(), inicio, fim);

            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            StringBuilder sb = new StringBuilder();
            sb.append("tipo,descricao,valor,dataHora\n");
            for (var m : list) {
                sb.append(m.getTipo()).append(',')
                  .append('"').append(m.getDescricao() == null ? "" : m.getDescricao().replace("\"", "\"\""))
                  .append('"').append(',')
                  .append(m.getValor()).append(',')
                  .append(m.getDataHora()).append('\n');
                total = total.add(m.getValor());
            }
            sb.append("\nTotal;").append(total.toString()).append('\n');

            byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(bytes);
            String filename = String.format("relatorio_diario_%s.csv", data.toString());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(bytes.length)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/relatorios/mes")
    public ResponseEntity<ByteArrayResource> relatorioMes(@RequestParam(name = "ano") int ano,
                                                          @RequestParam(name = "mes") int mes) {
        try {
            java.time.YearMonth ym = java.time.YearMonth.of(ano, mes);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null) return ResponseEntity.status(401).build();

            var optUser = usuarioService.buscarPorUsername(username);
            if (optUser.isEmpty()) return ResponseEntity.status(401).build();

            var user = optUser.get();
            var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
            if (optArtesao.isEmpty()) return ResponseEntity.badRequest().build();
            var artesao = optArtesao.get();

            var inicio = ym.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            var fim = ym.atEndOfMonth().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);

            var list = movimentacaoService.listarPorPeriodo(artesao.getId(), inicio, fim);

            // aggregate by day
            java.util.Map<java.time.LocalDate, java.math.BigDecimal> map = new java.util.TreeMap<>();
            for (var m : list) {
                var day = m.getDataHora().toLocalDate();
                map.putIfAbsent(day, java.math.BigDecimal.ZERO);
                map.put(day, map.get(day).add(m.getValor()));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("data,total\n");
            java.math.BigDecimal grand = java.math.BigDecimal.ZERO;
            for (var entry : map.entrySet()) {
                sb.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
                grand = grand.add(entry.getValue());
            }
            sb.append("\nTotal;").append(grand.toString()).append('\n');

            byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(bytes);
            String filename = String.format("relatorio_mensal_%04d-%02d.csv", ano, mes);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(bytes.length)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/relatorios/ano")
    public ResponseEntity<ByteArrayResource> relatorioAno(@RequestParam(name = "ano") int ano) {
        try {
            java.time.Year y = java.time.Year.of(ano);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null) return ResponseEntity.status(401).build();

            var optUser = usuarioService.buscarPorUsername(username);
            if (optUser.isEmpty()) return ResponseEntity.status(401).build();

            var user = optUser.get();
            var optArtesao = artesaoService.buscarPorUsuarioId(user.getId());
            if (optArtesao.isEmpty()) return ResponseEntity.badRequest().build();
            var artesao = optArtesao.get();

            var inicio = y.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime();
            var fim = y.atDay(y.length()).plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().minusNanos(1);

            var list = movimentacaoService.listarPorPeriodo(artesao.getId(), inicio, fim);

            // aggregate by month
            java.util.Map<java.time.Month, java.math.BigDecimal> map = new java.util.TreeMap<>();
            for (var m : list) {
                var mo = m.getDataHora().getMonth();
                map.putIfAbsent(mo, java.math.BigDecimal.ZERO);
                map.put(mo, map.get(mo).add(m.getValor()));
            }

            StringBuilder sb = new StringBuilder();
            sb.append("mes,total\n");
            java.math.BigDecimal grand = java.math.BigDecimal.ZERO;
            for (var entry : map.entrySet()) {
                sb.append(entry.getKey().getValue()).append('-').append(entry.getKey().name()).append(',').append(entry.getValue()).append('\n');
                grand = grand.add(entry.getValue());
            }
            sb.append("\nTotal;").append(grand.toString()).append('\n');

            byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ByteArrayResource resource = new ByteArrayResource(bytes);
            String filename = String.format("relatorio_anual_%04d.csv", ano);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentLength(bytes.length)
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

}
