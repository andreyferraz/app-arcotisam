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
  const total = rows.length;
  const pages = Math.max(1, Math.ceil(total / pageSize));
  let current = 1;

  function renderPage(page) {
    current = Math.min(Math.max(1, page), pages);
    const start = (current - 1) * pageSize;
    const end = start + pageSize;
    rows.forEach((r, i) => {
      r.style.display = (i >= start && i < end) ? '' : 'none';
    });
    renderControls();
  }

  function renderControls() {
    pager.innerHTML = '';
    if (pages <= 1) return;

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
    let endPage = Math.min(pages, startPage + windowSize - 1);
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
    next.disabled = current === pages;
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
  const product = products.find(item => item.id === productId);
  const itemInCart = cart.find(item => item.id === productId);

  if (itemInCart) {
    itemInCart.quantity += 1;
  } else {
    cart.push({ ...product, quantity: 1 });
  }

  saveCart();
  renderCart();
  updateCartCount();
}

function changeQuantity(productId, action) {
  const item = cart.find(product => product.id === productId);

  if (!item) return;

  if (action === "increase") {
    item.quantity += 1;
  }

  if (action === "decrease") {
    item.quantity -= 1;
  }

  if (item.quantity <= 0) {
    cart = cart.filter(product => product.id !== productId);
  }

  saveCart();
  renderCart();
  updateCartCount();
}

function removeFromCart(productId) {
  cart = cart.filter(product => product.id !== productId);
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
        <button type="button" onclick="changeQuantity(${item.id}, 'decrease')">−</button>
        <strong>${item.quantity}</strong>
        <button type="button" onclick="changeQuantity(${item.id}, 'increase')">+</button>
        <button class="remove-btn" type="button" onclick="removeFromCart(${item.id})">Remover</button>
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

  // initialize table pagination for server-rendered products list
  if (document.getElementById('produtosTbody')) {
    initTablePagination('produtosTable', 'produtosTbody', 'produtosPagination', 5);
  }
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
