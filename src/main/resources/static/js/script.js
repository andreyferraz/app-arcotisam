// ==========================================================
// ARCOTISAM - Template JavaScript puro
// Navegação por abas, loja, filtro e contato.
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

const navLinks = document.querySelectorAll(".nav-link");
const menuToggle = document.getElementById("menuToggle");
const navLinksBox = document.getElementById("navLinks");

// DOM nodes that depend on the currently loaded page
let productGrid = null;
let searchProduct = null;
let categoryFilter = null;
let contactForm = null;
let formMessage = null;
let contactWhatsapp = null;
let isProcessingImageOrientation = false;
let productImagePreview = null;
let productImagePreviewImg = null;
let productImageLoadToken = 0;
let shopImageLightbox = null;
let shopImageLightboxImg = null;
let shopImageLightboxClose = null;
let activeLightboxSource = null;
let activeGalleryRotationSubmit = false;

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
      <div class="admin-empty-state">
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

function normalizarWhatsapp(whatsapp) {
  if (!whatsapp) {
    return null;
  }

  let numeros = String(whatsapp).trim().replace(/\D/g, '');
  if (!numeros) {
    return null;
  }

  if (!numeros.startsWith('55')) {
    numeros = `55${numeros}`;
  }

  return numeros;
}

function abrirWhatsAppContato(nome, email, mensagem, whatsapp) {
  const numero = normalizarWhatsapp(whatsapp);
  if (!numero) {
    throw new Error('WhatsApp do contato nao encontrado.');
  }

  const texto = [
    'Olá! Gostaria de entrar em contato pela ARCOTISAM.',
    `Nome: ${nome}`,
    `E-mail: ${email}`,
    `Mensagem: ${mensagem}`
  ].join('\n');

  const url = `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
  window.open(url, '_blank', 'noopener,noreferrer');
}

function getRotationRadians(degrees) {
  return (Number(degrees) || 0) * Math.PI / 180;
}

async function rotateImageFile(file, degrees) {
  const rotation = Number(degrees) || 0;
  if (!file || !rotation || rotation % 360 === 0) {
    return file;
  }

  const imageUrl = URL.createObjectURL(file);
  try {
    const image = await new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('Nao foi possivel carregar a imagem para rotacao.'));
      img.src = imageUrl;
    });

    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');
    if (!context) {
      throw new Error('Nao foi possivel preparar a imagem para rotacao.');
    }

    const normalizedRotation = ((rotation % 360) + 360) % 360;
    const isQuarterTurn = normalizedRotation === 90 || normalizedRotation === 270;
    canvas.width = isQuarterTurn ? image.height : image.width;
    canvas.height = isQuarterTurn ? image.width : image.height;

    context.translate(canvas.width / 2, canvas.height / 2);
    context.rotate(getRotationRadians(normalizedRotation));
    context.drawImage(image, -image.width / 2, -image.height / 2);

    const blob = await new Promise((resolve, reject) => {
      canvas.toBlob(result => {
        if (result) {
          resolve(result);
        } else {
          reject(new Error('Nao foi possivel gerar a imagem rotacionada.'));
        }
      }, file.type || 'image/jpeg', 0.92);
    });

    return new File([blob], file.name, { type: blob.type || file.type || 'image/jpeg' });
  } finally {
    URL.revokeObjectURL(imageUrl);
  }
}

async function prepareImageOrientationInputs(form) {
  const selectors = Array.from(form.querySelectorAll('[data-image-orientation]'));
  for (const selector of selectors) {
    const field = selector.closest('label, .admin-upload, div, form')?.querySelector('input[type="file"][accept*="image"]') || form.querySelector('input[type="file"][accept*="image"]');
    if (!field || !field.files || !field.files.length) {
      continue;
    }

    const rotation = selector.value;
    const original = field.files[0];
    const rotated = await rotateImageFile(original, rotation);
    if (rotated !== original) {
      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(rotated);
      field.files = dataTransfer.files;
    }
  }
}

async function prepareGalleryRotations(form) {
  const fileInput = form.querySelector('input[type="file"][name="fotos"]');
  if (!fileInput) {
    return;
  }

  const dataTransfer = new DataTransfer();
  const existingFiles = fileInput.files ? Array.from(fileInput.files) : [];
  existingFiles.forEach(file => dataTransfer.items.add(file));

  const removeValues = new Set(Array.from(form.querySelectorAll('input[name="fotosRemover"]:checked')).map(input => input.value));
  const rotationControls = Array.from(form.querySelectorAll('[data-gallery-rotation]'));

  for (const control of rotationControls) {
    const rotation = Number(control.value) || 0;
    const sourceFile = control.getAttribute('data-gallery-source');
    const fullSrc = control.getAttribute('data-gallery-fullsrc');
    if (!sourceFile || !fullSrc || rotation % 360 === 0 || removeValues.has(sourceFile)) {
      continue;
    }

    const rotatedFile = await fetchRotatedGalleryFile(fullSrc, rotation);
    dataTransfer.items.add(rotatedFile);
    removeValues.add(sourceFile);
  }

  fileInput.files = dataTransfer.files;

  removeValues.forEach(value => {
    const checkbox = form.querySelector(`input[name="fotosRemover"][value="${CSS.escape(value)}"]`);
    if (checkbox) {
      checkbox.checked = true;
    }
  });
}

function atualizarPreviewRotacaoGaleria(select) {
  const rotation = Number(select.value) || 0;
  const previewKey = select.getAttribute('data-gallery-preview');
  if (!previewKey) {
    return;
  }

  const previewImage = document.querySelector(`img[data-gallery-preview="${CSS.escape(previewKey)}"]`);
  if (!previewImage) {
    return;
  }

  previewImage.style.transform = `rotate(${rotation}deg)`;
}

function updateImagePreview(previewContainer, previewImage, file, rotationDegrees) {
  if (!previewContainer || !previewImage || !file) {
    return;
  }

  const previewUrl = URL.createObjectURL(file);
  previewImage.onload = () => {
    previewImage.style.transform = `rotate(${Number(rotationDegrees) || 0}deg)`;
    previewContainer.style.display = 'block';
    URL.revokeObjectURL(previewUrl);
  };
  previewImage.onerror = () => {
    previewContainer.style.display = 'none';
    URL.revokeObjectURL(previewUrl);
  };
  previewImage.src = previewUrl;
}

async function loadImageIntoFieldFromUrl(fileInput, imageUrl, previewContainer, previewImage) {
  if (!fileInput || !imageUrl) {
    return;
  }

  const currentToken = ++productImageLoadToken;
  const response = await fetch(imageUrl, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error('Nao foi possivel carregar a imagem atual do produto.');
  }

  const blob = await response.blob();
  if (currentToken !== productImageLoadToken) {
    return;
  }

  const filename = imageUrl.split('/').pop() || 'imagem.jpg';
  const mimeType = blob.type || 'image/jpeg';
  const file = new File([blob], filename, { type: mimeType });
  const dataTransfer = new DataTransfer();
  dataTransfer.items.add(file);
  fileInput.files = dataTransfer.files;

  updateImagePreview(previewContainer, previewImage, file, 0);
}

async function fetchRotatedGalleryFile(imageUrl, degrees) {
  const response = await fetch(imageUrl, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error('Nao foi possivel carregar uma imagem da galeria.');
  }

  const blob = await response.blob();
  const originalName = imageUrl.split('/').pop() || 'galeria.jpg';
  const originalFile = new File([blob], originalName, { type: blob.type || 'image/jpeg' });
  return rotateImageFile(originalFile, degrees);
}

function openShopImageLightbox(imageUrl, altText) {
  if (!shopImageLightbox || !shopImageLightboxImg) {
    return;
  }

  activeLightboxSource = imageUrl;
  shopImageLightboxImg.src = imageUrl;
  shopImageLightboxImg.alt = altText || 'Imagem ampliada do produto';
  shopImageLightbox.classList.add('is-open');
  shopImageLightbox.setAttribute('aria-hidden', 'false');
  document.body.classList.add('lightbox-open');
}

function closeShopImageLightbox() {
  if (!shopImageLightbox || !shopImageLightboxImg) {
    return;
  }

  shopImageLightbox.classList.remove('is-open');
  shopImageLightbox.setAttribute('aria-hidden', 'true');
  shopImageLightboxImg.removeAttribute('src');
  document.body.classList.remove('lightbox-open');
  activeLightboxSource = null;
}

// post-load initialization for page-specific elements
function postLoadInit(page) {
  productGrid = document.getElementById("productGrid");
  searchProduct = document.getElementById("searchProduct");
  categoryFilter = document.getElementById("categoryFilter");
  contactForm = document.getElementById("contactForm");
  formMessage = document.getElementById("formMessage");
  contactWhatsapp = document.querySelector("[data-whatsapp]");
  productImagePreview = document.getElementById('produtoImagemPreview');
  productImagePreviewImg = document.getElementById('produtoImagemPreviewImg');
  shopImageLightbox = document.getElementById('shopImageLightbox');
  shopImageLightboxImg = document.getElementById('shopImageLightboxImg');
  shopImageLightboxClose = document.getElementById('shopImageLightboxClose');

  if (searchProduct) searchProduct.addEventListener("input", renderProducts);
  if (categoryFilter) categoryFilter.addEventListener("change", renderProducts);

  if (contactForm) {
    contactForm.addEventListener("submit", event => {
      event.preventDefault();

      const nome = contactForm.nome.value.trim();
      const email = contactForm.email.value.trim();
      const mensagem = contactForm.mensagem.value.trim();
      const whatsapp = contactWhatsapp && contactWhatsapp.dataset ? contactWhatsapp.dataset.whatsapp : null;

      try {
        abrirWhatsAppContato(nome, email, mensagem, whatsapp);

        if (formMessage) formMessage.textContent = "Abrindo WhatsApp com sua mensagem...";
        contactForm.reset();

        setTimeout(() => {
          if (formMessage) formMessage.textContent = "";
        }, 4000);
      } catch (error) {
        if (formMessage) formMessage.textContent = error.message;
      }
    });
  }

  if (shopImageLightbox && !shopImageLightbox.dataset.bound) {
    shopImageLightbox.dataset.bound = 'true';

    document.addEventListener('click', event => {
      const image = event.target.closest && event.target.closest('.shop-product-photo, .gallery-expand-photo');
      if (!image) {
        return;
      }

      const fullSrc = image.getAttribute('data-fullsrc') || image.getAttribute('src');
      if (fullSrc) {
        openShopImageLightbox(fullSrc, image.getAttribute('alt'));
      }
    });

    document.addEventListener('keydown', event => {
      const target = event.target;
      if ((event.key === 'Enter' || event.key === ' ') && target && target.closest && target.closest('.gallery-expand-photo, .shop-product-photo')) {
        event.preventDefault();
        const image = target.closest('.gallery-expand-photo, .shop-product-photo');
        const fullSrc = image.getAttribute('data-fullsrc') || image.getAttribute('src');
        if (fullSrc) {
          openShopImageLightbox(fullSrc, image.getAttribute('alt'));
        }
      }
    });

    document.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        closeShopImageLightbox();
      }
    });

    if (shopImageLightboxClose) {
      shopImageLightboxClose.addEventListener('click', closeShopImageLightbox);
    }

    shopImageLightbox.addEventListener('click', event => {
      if (event.target === shopImageLightbox) {
        closeShopImageLightbox();
      }
    });
  }

  const imageForms = Array.from(document.querySelectorAll('form[enctype="multipart/form-data"]'));
  imageForms.forEach(form => {
    if (form.dataset.orientationBound === 'true') {
      return;
    }

    form.dataset.orientationBound = 'true';
    form.addEventListener('submit', async event => {
      if (isProcessingImageOrientation) {
        return;
      }

      if (activeGalleryRotationSubmit) {
        return;
      }

      const selectors = form.querySelectorAll('[data-image-orientation]');
      if (!selectors.length) {
        const galleryRotations = form.querySelectorAll('[data-gallery-rotation]');
        if (!galleryRotations.length) {
          return;
        }
      }

      event.preventDefault();
      if (form.querySelectorAll('[data-gallery-rotation]').length) {
        activeGalleryRotationSubmit = true;
      } else {
        isProcessingImageOrientation = true;
      }

      try {
        if (form.querySelectorAll('[data-gallery-rotation]').length) {
          await prepareGalleryRotations(form);
          form.submit();
          return;
        }

        await prepareImageOrientationInputs(form);
        form.submit();
      } catch (error) {
        if (formMessage) {
          formMessage.textContent = error.message;
        }
      } finally {
        isProcessingImageOrientation = false;
        activeGalleryRotationSubmit = false;
      }
    });
  });

  document.querySelectorAll('[data-gallery-rotation]').forEach(select => {
    if (select.dataset.rotationBound === 'true') {
      return;
    }

    select.dataset.rotationBound = 'true';
    select.addEventListener('change', () => {
      atualizarPreviewRotacaoGaleria(select);
    });

    atualizarPreviewRotacaoGaleria(select);
  });

  // render/update after elements are attached
  renderProducts();

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
  const inputImagemOrientacao = form.querySelector('[data-image-orientation]');
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
    const imagemUrl = btn.getAttribute('data-imagemurl');

    inputId.value = id || '';
    inputNome.value = nome || '';
    inputDescricao.value = descricao || '';
    inputPreco.value = preco || '';
    if (inputImagem) inputImagem.value = '';
    if (inputImagemOrientacao) inputImagemOrientacao.value = '0';

    form.action = '/artesao/produtos/' + id + '/atualizar';
    submitBtn.textContent = 'Salvar Alterações';
    cancelBtn.style.display = '';

    if (imagemUrl) {
      loadImageIntoFieldFromUrl(inputImagem, `/uploads/${imagemUrl}`, productImagePreview, productImagePreviewImg)
        .catch(() => {
          if (productImagePreview) {
            productImagePreview.style.display = 'none';
          }
        });
    } else if (productImagePreview) {
      productImagePreview.style.display = 'none';
      if (productImagePreviewImg) {
        productImagePreviewImg.removeAttribute('src');
      }
    }
  });

  if (inputImagem) {
    inputImagem.addEventListener('change', () => {
      const file = inputImagem.files && inputImagem.files[0];
      if (!file) {
        if (productImagePreview) productImagePreview.style.display = 'none';
        return;
      }

      updateImagePreview(productImagePreview, productImagePreviewImg, file, inputImagemOrientacao ? inputImagemOrientacao.value : 0);
    });
  }

  if (inputImagemOrientacao) {
    inputImagemOrientacao.addEventListener('change', () => {
      const file = inputImagem && inputImagem.files ? inputImagem.files[0] : null;
      if (!file) {
        return;
      }

      updateImagePreview(productImagePreview, productImagePreviewImg, file, inputImagemOrientacao.value);
    });
  }

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
  const inputImagemOrientacao = form.querySelector('[data-image-orientation]');
  const submitBtn = document.getElementById('produtoSubmit');
  const cancelBtn = document.getElementById('produtoCancel');

  if (inputId) inputId.value = '';
  if (inputNome) inputNome.value = '';
  if (inputDescricao) inputDescricao.value = '';
  if (inputPreco) inputPreco.value = '';
  if (inputImagem) inputImagem.value = '';
  if (inputImagemOrientacao) inputImagemOrientacao.value = '0';
  if (submitBtn) submitBtn.textContent = 'Cadastrar Produto';
  if (cancelBtn) cancelBtn.style.display = 'none';
  if (productImagePreview) productImagePreview.style.display = 'none';
  if (productImagePreviewImg) productImagePreviewImg.removeAttribute('src');
}

function initBlogEditor() {
  const editor = document.getElementById('blogEditor');
  const output = document.getElementById('conteudoHtml');
  const form = document.querySelector('.blog-form');
  const toolbar = document.querySelector('.blog-editor-toolbar');

  if (!editor || !output || !form || !toolbar) {
    return;
  }

  const syncContent = () => {
    output.value = editor.innerHTML.trim();
  };

  toolbar.addEventListener('mousedown', event => {
    const button = event.target.closest('button[data-command]');
    if (!button) return;
    event.preventDefault();
  });

  toolbar.addEventListener('click', event => {
    const button = event.target.closest('button[data-command]');
    if (!button) return;

    const command = button.getAttribute('data-command');
    editor.focus();

    if (command === 'createLink') {
      const url = window.prompt('Informe a URL do link:');
      if (url) {
        document.execCommand('createLink', false, url.trim());
      }
    } else {
      document.execCommand(command, false, null);
    }

    syncContent();
  });

  editor.addEventListener('input', syncContent);
  form.addEventListener('submit', syncContent);
  syncContent();
}

document.getElementById("currentYear").textContent = new Date().getFullYear();

// Initialize page-specific behaviors for server-rendered pages
postLoadInit();
initBlogEditor();

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
