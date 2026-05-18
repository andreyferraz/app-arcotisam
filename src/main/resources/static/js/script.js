// ==========================================================
// ARCOTISAM - Template JavaScript puro
// Navegação por abas, loja, filtro, carrinho e contato.
// ==========================================================

const products = [
  {
    id: 1,
    name: "Vaso Cerâmico Artesanal",
    category: "decoracao",
    description: "Peça decorativa modelada manualmente com acabamento rústico.",
    price: 89.90,
    icon: "🏺"
  },
  {
    id: 2,
    name: "Bolsa de Palha Natural",
    category: "moda",
    description: "Bolsa leve, resistente e inspirada nas texturas da natureza.",
    price: 129.90,
    icon: "👜"
  },
  {
    id: 3,
    name: "Jogo Americano Têxtil",
    category: "utilitarios",
    description: "Conjunto artesanal para mesa posta, com acabamento em tecido.",
    price: 74.90,
    icon: "🧵"
  },
  {
    id: 4,
    name: "Escultura em Madeira",
    category: "decoracao",
    description: "Objeto decorativo produzido em madeira com traços orgânicos.",
    price: 159.90,
    icon: "🪵"
  },
  {
    id: 5,
    name: "Colar Artesanal",
    category: "moda",
    description: "Acessório autoral com inspiração em elementos naturais.",
    price: 49.90,
    icon: "📿"
  },
  {
    id: 6,
    name: "Cesto Multiuso",
    category: "utilitarios",
    description: "Cesto trançado para organização, decoração e uso diário.",
    price: 99.90,
    icon: "🧺"
  }
];

let cart = JSON.parse(localStorage.getItem("arcotisam_cart")) || [];

const navLinks = document.querySelectorAll(".nav-link");
const menuToggle = document.getElementById("menuToggle");
const navLinksBox = document.getElementById("navLinks");
const cartCount = document.getElementById("cartCount");

// DOM nodes that depend on the currently loaded page
let productGrid = null;
let searchProduct = null;
let categoryFilter = null;
let cartItems = null;
let subtotal = null;
let total = null;
let checkoutBtn = null;
let contactForm = null;
let formMessage = null;

function formatCurrency(value) {
  return value.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL"
  });
}

// No client-side route interception: header links perform full page loads (server-side Thymeleaf).
// Keep navLinks variable in case of UI enhancements, but do not prevent default navigation.

menuToggle.addEventListener("click", () => {
  const isOpen = navLinksBox.classList.toggle("open");
  menuToggle.setAttribute("aria-expanded", String(isOpen));
});

function renderProducts() {
  if (!productGrid) return;
  const search = (searchProduct && searchProduct.value || '').toLowerCase().trim();
  const category = (categoryFilter && categoryFilter.value) || 'todos';

  const filteredProducts = products.filter(product => {
    const matchesSearch = product.name.toLowerCase().includes(search)
      || product.description.toLowerCase().includes(search);

    const matchesCategory = category === "todos" || product.category === category;

    return matchesSearch && matchesCategory;
  });

  if (!filteredProducts.length) {
    productGrid.innerHTML = `
      <div class="cart-empty">
        Nenhum produto encontrado para o filtro selecionado.
      </div>
    `;
    return;
  }

  productGrid.innerHTML = filteredProducts.map(product => `
    <article class="product-card">
      <div class="product-image">${product.icon}</div>
      <div class="product-body">
        <span class="product-category">${getCategoryLabel(product.category)}</span>
        <h3>${product.name}</h3>
        <p>${product.description}</p>
        <div class="product-footer">
          <strong class="price">${formatCurrency(product.price)}</strong>
          <button class="btn btn-primary" type="button" onclick="addToCart(${product.id})">
            Adicionar
          </button>
        </div>
      </div>
    </article>
  `).join("");
}

/* Table pagination utility for server-rendered tables
   - tableId: id of the <table>
   - tbodyId: id of the <tbody> containing rows
   - pagerId: id of the container where pagination controls will be rendered
   - pageSize: number of rows per page
*/
function initTablePagination(tableId, tbodyId, pagerId, pageSize = 5) {
  const tbody = document.getElementById(tbodyId);
  const pager = document.getElementById(pagerId);
  if (!tbody || !pager) return;

  const rows = Array.from(tbody.querySelectorAll('tr'));
  // pagination will consider only rows that are NOT marked with 'filter-hidden'
  const visibleRows = () => rows.filter(r => !r.classList.contains('filter-hidden'));
  const total = () => visibleRows().length;
  const pages = () => Math.max(1, Math.ceil(total() / pageSize));
  let current = 1;

  function renderPage(page) {
    current = Math.min(Math.max(1, page), pages());
    const start = (current - 1) * pageSize;
    const end = start + pageSize;
    const vr = visibleRows();
    // hide all rows first
    rows.forEach(r => { r.style.display = 'none'; });
    // show only the page slice of visible rows
    vr.forEach((r, idx) => {
      if (idx >= start && idx < end) r.style.display = '';
      else r.style.display = 'none';
    });
    renderControls();
  }

  function renderControls() {
    pager.innerHTML = '';
    if (pages() <= 1) return;

    // info text: Página X de Y
    const info = document.createElement('span');
    info.className = 'pagination-info';
    info.textContent = `Página ${current} de ${pages()}`;
    pager.appendChild(info);

    const prev = document.createElement('button');
    prev.type = 'button';
    prev.className = 'btn btn-sm';
    prev.textContent = '«';
    prev.disabled = current === 1;
    prev.addEventListener('click', () => renderPage(current - 1));
    pager.appendChild(prev);

    // page numbers (limit to a small window)
    const windowSize = 5;
    let startPage = Math.max(1, current - Math.floor(windowSize / 2));
    let endPage = Math.min(pages(), startPage + windowSize - 1);
    if (endPage - startPage + 1 < windowSize) {
      startPage = Math.max(1, endPage - windowSize + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'btn btn-sm' + (i === current ? ' active' : '');
      btn.textContent = String(i);
      btn.addEventListener('click', () => renderPage(i));
      pager.appendChild(btn);
    }

    const next = document.createElement('button');
    next.type = 'button';
    next.className = 'btn btn-sm';
    next.textContent = '»';
    next.disabled = current === pages();
    next.addEventListener('click', () => renderPage(current + 1));
    pager.appendChild(next);
  }

  // initialize
  renderPage(1);
}

function getCategoryLabel(category) {
  const categories = {
    decoracao: "Decoração",
    moda: "Moda",
    utilitarios: "Utilitários"
  };

  return categories[category] || category;
}

function saveCart() {
  localStorage.setItem("arcotisam_cart", JSON.stringify(cart));
}

function addToCart(productId) {
  const product = products.find(item => String(item.id) === String(productId));
  const itemInCart = cart.find(item => String(item.id) === String(productId));

  if (itemInCart) {
    itemInCart.quantity += 1;
  } else {
    cart.push({ ...product, quantity: 1 });
  }

  saveCart();
  renderCart();
  updateCartCount();
}

// Adiciona produto vindo do HTML server-rendered (quando não existe na lista `products` do cliente)
function addServerProductToCartFromCardData(productId, btn) {
  const card = btn.closest('.product-card');
  if (!card) return;

  const name = (card.querySelector('.product-title')?.textContent || '').trim();
  const priceText = (card.querySelector('.price')?.textContent || '').trim();
  let price = 0;
  if (priceText) {
    const cleaned = priceText.replace(/[^0-9,.-]/g, '').replace(',', '.');
    price = parseFloat(cleaned) || 0;
  }

  const imgEl = card.querySelector('.product-image img');
  let icon = '🧶';
  if (imgEl) {
    const src = imgEl.getAttribute('src') || imgEl.getAttribute('data-src') || imgEl.src;
    if (src) icon = `<img src="${src}" alt="${name}" style="width:48px;height:48px;object-fit:cover;border-radius:8px;">`;
  } else {
    const txt = (card.querySelector('.product-image')?.textContent || '').trim();
    if (txt) icon = txt;
  }

  const existing = cart.find(item => String(item.id) === String(productId));
  if (existing) {
    existing.quantity += 1;
  } else {
    cart.push({ id: String(productId), name: name || 'Produto', price: price, icon: icon, quantity: 1 });
  }

  saveCart();
  renderCart();
  updateCartCount();
}

function changeQuantity(productId, action) {
  const item = cart.find(product => String(product.id) === String(productId));

  if (!item) return;

  if (action === "increase") {
    item.quantity += 1;
  }

  if (action === "decrease") {
    item.quantity -= 1;
  }

  if (item.quantity <= 0) {
    cart = cart.filter(product => String(product.id) !== String(productId));
  }

  saveCart();
  renderCart();
  updateCartCount();
}

function removeFromCart(productId) {
  cart = cart.filter(product => String(product.id) !== String(productId));
  saveCart();
  renderCart();
  updateCartCount();
}

function updateCartCount() {
  const quantity = cart.reduce((sum, item) => sum + item.quantity, 0);
  if (cartCount) cartCount.textContent = quantity;
}

function renderCart() {
  if (!cartItems) return;

  if (!cart.length) {
    cartItems.innerHTML = `
      <div class="cart-empty">
        Seu carrinho está vazio. Acesse a loja para adicionar produtos.
      </div>
    `;

    if (subtotal) subtotal.textContent = formatCurrency(0);
    if (total) total.textContent = formatCurrency(0);
    return;
  }

  cartItems.innerHTML = cart.map(item => `
    <article class="cart-item">
      <div class="cart-item-icon">${item.icon}</div>
      <div>
        <h4>${item.name}</h4>
        <p>${formatCurrency(item.price)} por unidade</p>
      </div>
      <div class="quantity-control">
        <button type="button" data-cart-action="decrease" data-cart-id="${String(item.id)}">−</button>
        <strong>${item.quantity}</strong>
        <button type="button" data-cart-action="increase" data-cart-id="${String(item.id)}">+</button>
        <button class="remove-btn" type="button" data-cart-action="remove" data-cart-id="${String(item.id)}" aria-label="Remover item">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M3 6h18v2H3V6zm2 3h2v11H5V9zm4 0h2v11H9V9zm4 0h2v11h-2V9zm4 0h2v11h-2V9zM8 2h8l-1 2H9L8 2z"/>
          </svg>
        </button>
      </div>
    </article>
  `).join("");

  const subtotalValue = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);

  if (subtotal) subtotal.textContent = formatCurrency(subtotalValue);
  if (total) total.textContent = formatCurrency(subtotalValue);
}

// post-load initialization for page-specific elements
function postLoadInit(page) {
  productGrid = document.getElementById("productGrid");
  searchProduct = document.getElementById("searchProduct");
  categoryFilter = document.getElementById("categoryFilter");
  cartItems = document.getElementById("cartItems");
  subtotal = document.getElementById("subtotal");
  total = document.getElementById("total");
  checkoutBtn = document.getElementById("checkoutBtn");
  contactForm = document.getElementById("contactForm");
  formMessage = document.getElementById("formMessage");

  if (searchProduct) searchProduct.addEventListener("input", renderProducts);
  if (categoryFilter) categoryFilter.addEventListener("change", renderProducts);

  if (checkoutBtn) {
    checkoutBtn.addEventListener("click", () => {
      if (!cart.length) {
        alert("Seu carrinho está vazio.");
        return;
      }

      alert("Pedido simulado com sucesso! Integre aqui com seu backend ou gateway de pagamento.");
    });
  }

  if (contactForm) {
    contactForm.addEventListener("submit", event => {
      event.preventDefault();

      if (formMessage) formMessage.textContent = "Mensagem enviada com sucesso! Este é apenas um envio simulado.";
      contactForm.reset();

      setTimeout(() => {
        if (formMessage) formMessage.textContent = "";
      }, 4000);
    });
  }

  // render/update after elements are attached
  renderProducts();
  renderCart();
  updateCartCount();

  if (cartItems && !cartItems.dataset.bound) {
    cartItems.dataset.bound = "true";
    cartItems.addEventListener("click", (ev) => {
      const btn = ev.target.closest("button[data-cart-action]");
      if (!btn) return;

      ev.preventDefault();

      const action = btn.dataset.cartAction;
      const productId = btn.dataset.cartId;

      if (action === "increase") {
        changeQuantity(productId, "increase");
      }

      if (action === "decrease") {
        changeQuantity(productId, "decrease");
      }

      if (action === "remove") {
        removeFromCart(productId);
      }
    });
  }

  // Delegação global para botões de adicionar em server-rendered pages
  document.addEventListener('click', (ev) => {
    const btn = ev.target.closest && ev.target.closest('.buy-btn');
    if (!btn) return;
    ev.preventDefault();
    const idAttr = btn.getAttribute('data-id');
    const numericId = (idAttr && !isNaN(Number(idAttr))) ? Number(idAttr) : idAttr;

    // se o produto está na lista cliente, reutiliza addToCart
    if (products.find(p => p.id === numericId)) {
      addToCart(numericId);
    } else {
      addServerProductToCartFromCardData(idAttr, btn);
    }
  });

  // initialize table pagination for server-rendered products list
  if (document.getElementById('produtosTbody')) {
    initTablePagination('produtosTable', 'produtosTbody', 'produtosPagination', 5);
  }

  // init edit buttons for produtos table (server-rendered rows)
  initProdutoEditButtons();

  // initialize product search input if present
  const produtoSearch = document.getElementById('produtoSearch');
  if (produtoSearch) {
    produtoSearch.addEventListener('input', () => {
      applyProdutoSearch(produtoSearch.value.trim().toLowerCase());
      // re-init pagination to reflect filtered results
      initTablePagination('produtosTable', 'produtosTbody', 'produtosPagination', 5);
    });
  }
}

function initProdutoEditButtons() {
  const tbody = document.getElementById('produtosTbody');
  if (!tbody) return;

  const form = document.querySelector('.product-form');
  const inputId = document.getElementById('produtoId');
  const inputNome = form.querySelector('input[name="nome"]');
  const inputDescricao = form.querySelector('input[name="descricao"]');
  const inputPreco = form.querySelector('input[name="preco"]');
  const inputImagem = form.querySelector('input[name="imagem"]');
  const submitBtn = document.getElementById('produtoSubmit');
  const cancelBtn = document.getElementById('produtoCancel');

  // event delegation on tbody for edit buttons
  tbody.addEventListener('click', (ev) => {
    const btn = ev.target.closest && ev.target.closest('.edit-btn');
    if (!btn) return;
    const id = btn.getAttribute('data-id');
    const nome = btn.getAttribute('data-nome');
    const descricao = btn.getAttribute('data-descricao');
    const preco = btn.getAttribute('data-preco');

    inputId.value = id || '';
    inputNome.value = nome || '';
    inputDescricao.value = descricao || '';
    inputPreco.value = preco || '';
    if (inputImagem) inputImagem.value = '';

    form.action = '/artesao/produtos/' + id + '/atualizar';
    submitBtn.textContent = 'Salvar Alterações';
    cancelBtn.style.display = '';
  });

  if (cancelBtn) {
    cancelBtn.addEventListener('click', () => {
      resetProdutoForm();
    });
  }
}

function applyProdutoSearch(query) {
  const tbody = document.getElementById('produtosTbody');
  if (!tbody) return;
  const rows = Array.from(tbody.querySelectorAll('tr'));
  rows.forEach(row => {
    const nomeCell = row.querySelectorAll('td')[1];
    const nomeText = nomeCell ? nomeCell.textContent.trim().toLowerCase() : '';
    if (!query) {
      row.classList.remove('filter-hidden');
    } else {
      if (nomeText.indexOf(query) === -1) row.classList.add('filter-hidden');
      else row.classList.remove('filter-hidden');
    }
  });
}

function resetProdutoForm() {
  const form = document.querySelector('.product-form');
  if (!form) return;
  form.action = '/artesao/produtos/salvar';
  const inputId = document.getElementById('produtoId');
  const inputNome = form.querySelector('input[name="nome"]');
  const inputDescricao = form.querySelector('input[name="descricao"]');
  const inputPreco = form.querySelector('input[name="preco"]');
  const inputImagem = form.querySelector('input[name="imagem"]');
  const submitBtn = document.getElementById('produtoSubmit');
  const cancelBtn = document.getElementById('produtoCancel');

  if (inputId) inputId.value = '';
  if (inputNome) inputNome.value = '';
  if (inputDescricao) inputDescricao.value = '';
  if (inputPreco) inputPreco.value = '';
  if (inputImagem) inputImagem.value = '';
  if (submitBtn) submitBtn.textContent = 'Cadastrar Produto';
  if (cancelBtn) cancelBtn.style.display = 'none';
}

document.getElementById("currentYear").textContent = new Date().getFullYear();

// Initialize page-specific behaviors for server-rendered pages
postLoadInit();

// Marca a aba do menu como ativa com base no path atual
function markActiveNavLink() {
  const path = window.location.pathname.replace(/\/+$/,'') || '/';
  navLinks.forEach(link => {
    try {
      const hrefPath = new URL(link.href, location.origin).pathname.replace(/\/+$/,'') || '/';
      link.classList.toggle('active', hrefPath === path);
    } catch (e) {
      // fallback: comparar href literal
      const href = link.getAttribute('href');
      link.classList.toggle('active', href === path || (href === '/' && path === '/'));
    }
  });
}

// Atualiza ativo ao carregar e quando o histórico muda
markActiveNavLink();
window.addEventListener('popstate', markActiveNavLink);
