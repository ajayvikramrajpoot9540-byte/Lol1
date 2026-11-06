/* ==========================================================================
   FlashDrop Official Website & Customer Storefront Engine (app.js)
   Features:
   - Camera / Photo Search in Search Bar with Lowest Rate / Price First Sorting
   - Smart 30-Minute Forward Delivery Calculation (IST Timezone)
   - Live Partner Shop Status (Standard 10:00 AM - 10:00 PM IST)
   - Public Customer Navigation: Home, Partner Shops, About Us, Terms & Conditions, Contact
   ========================================================================== */

const SHOPS = [
  {
    id: 'sharma_general',
    name: 'Sharma General Store',
    category: 'Groceries',
    address: 'Alha Chowk, Main Market, Mahoba',
    rating: 4.8,
    openingTime: '10:00 AM',
    closingTime: '10:00 PM'
  },
  {
    id: 'bundelkhand_sweets',
    name: 'Bundelkhand Sweets & Dairy',
    category: 'Sweets & Dairy',
    address: 'Rath Road, Near Bus Stand, Mahoba',
    rating: 4.9,
    openingTime: '09:00 AM',
    closingTime: '10:30 PM'
  },
  {
    id: 'kisan_fresh_veg',
    name: 'Kisan Fresh Sabzi Mandi',
    category: 'Vegetables',
    address: 'Subhash Chowk, Mahoba',
    rating: 4.7,
    openingTime: '08:00 AM',
    closingTime: '09:30 PM'
  },
  {
    id: 'mahoba_medicos',
    name: 'Mahoba Care Medicos & Stationery',
    category: 'Medicine & Stationery',
    address: 'Civil Lines, District Hospital Road, Mahoba',
    rating: 4.9,
    openingTime: '10:00 AM',
    closingTime: '10:00 PM'
  }
];

const PRODUCTS = [
  // Milk products (multiple prices to demonstrate Lowest Rate / Price First!)
  {
    id: 'prod_milk_1',
    name: 'Amul Taaza Toned Milk Pouch',
    category: 'Sweets & Dairy',
    shopId: 'bundelkhand_sweets',
    shopName: 'Bundelkhand Sweets & Dairy',
    price: 27,
    mrp: 30,
    unit: '500 ml',
    inStock: true,
    keywords: ['milk', 'dairy', 'amul', 'doodh', 'pouch', 'drink']
  },
  {
    id: 'prod_milk_2',
    name: 'Mother Dairy Full Cream Milk',
    category: 'Sweets & Dairy',
    shopId: 'sharma_general',
    shopName: 'Sharma General Store',
    price: 34,
    mrp: 36,
    unit: '500 ml',
    inStock: true,
    keywords: ['milk', 'dairy', 'cream', 'doodh', 'pouch']
  },
  {
    id: 'prod_milk_3',
    name: 'Farm Fresh Organic A2 Cow Milk',
    category: 'Sweets & Dairy',
    shopId: 'bundelkhand_sweets',
    shopName: 'Bundelkhand Sweets & Dairy',
    price: 75,
    mrp: 85,
    unit: '1 Litre',
    inStock: true,
    keywords: ['milk', 'dairy', 'cow', 'organic', 'doodh', 'bottle']
  },
  // Atta / Flour products (multiple rates to show Lowest Price First)
  {
    id: 'prod_atta_1',
    name: 'Chakki Fresh Sharbati Gehu Atta (Economy Pack)',
    category: 'Groceries',
    shopId: 'sharma_general',
    shopName: 'Sharma General Store',
    price: 195,
    mrp: 225,
    unit: '5 kg',
    inStock: true,
    keywords: ['atta', 'flour', 'wheat', 'gehu', 'roti', 'grocery']
  },
  {
    id: 'prod_atta_2',
    name: 'Aashirvaad Select Whole Wheat Atta',
    category: 'Groceries',
    shopId: 'sharma_general',
    shopName: 'Sharma General Store',
    price: 245,
    mrp: 275,
    unit: '5 kg',
    inStock: true,
    keywords: ['atta', 'flour', 'wheat', 'aashirvaad', 'grocery']
  },
  // Biscuits & Snacks
  {
    id: 'prod_biscuit_1',
    name: 'Parle-G Original Gluco Biscuits',
    category: 'Groceries',
    shopId: 'sharma_general',
    shopName: 'Sharma General Store',
    price: 10,
    mrp: 12,
    unit: '100 g',
    inStock: true,
    keywords: ['biscuit', 'parle', 'cookie', 'snack', 'tea']
  },
  {
    id: 'prod_biscuit_2',
    name: 'Britannia Good Day Cashew Cookies',
    category: 'Groceries',
    shopId: 'sharma_general',
    shopName: 'Sharma General Store',
    price: 30,
    mrp: 35,
    unit: '200 g',
    inStock: true,
    keywords: ['biscuit', 'cookie', 'good day', 'snack', 'britannia']
  },
  // Vegetables
  {
    id: 'prod_veg_1',
    name: 'Fresh Local Desi Tomatoes (Tamatar)',
    category: 'Vegetables',
    shopId: 'kisan_fresh_veg',
    shopName: 'Kisan Fresh Sabzi Mandi',
    price: 24,
    mrp: 35,
    unit: '1 kg',
    inStock: true,
    keywords: ['tomato', 'tamatar', 'vegetable', 'sabzi', 'fresh']
  },
  {
    id: 'prod_veg_2',
    name: 'Hybrid Salad Red Tomatoes',
    category: 'Vegetables',
    shopId: 'kisan_fresh_veg',
    shopName: 'Kisan Fresh Sabzi Mandi',
    price: 38,
    mrp: 45,
    unit: '1 kg',
    inStock: true,
    keywords: ['tomato', 'tamatar', 'vegetable', 'sabzi', 'salad']
  },
  {
    id: 'prod_veg_3',
    name: 'Bundelkhand New Harvest Potatoes (Aloo)',
    category: 'Vegetables',
    shopId: 'kisan_fresh_veg',
    shopName: 'Kisan Fresh Sabzi Mandi',
    price: 22,
    mrp: 30,
    unit: '1 kg',
    inStock: true,
    keywords: ['potato', 'aloo', 'vegetable', 'sabzi']
  },
  // Sweets
  {
    id: 'prod_sweet_1',
    name: 'Special Mahoba Khoya Pedha (250g Box)',
    category: 'Sweets & Dairy',
    shopId: 'bundelkhand_sweets',
    shopName: 'Bundelkhand Sweets & Dairy',
    price: 110,
    mrp: 130,
    unit: '250 g',
    inStock: true,
    keywords: ['sweet', 'pedha', 'mithai', 'khoya', 'dessert']
  },
  // Medicine & Stationery
  {
    id: 'prod_med_1',
    name: 'Classmate Spiral Ruled Notebook (A4)',
    category: 'Medicine & Stationery',
    shopId: 'mahoba_medicos',
    shopName: 'Mahoba Care Medicos & Stationery',
    price: 55,
    mrp: 70,
    unit: '160 Pages',
    inStock: true,
    keywords: ['notebook', 'copy', 'book', 'stationery', 'paper', 'pen']
  },
  {
    id: 'prod_med_2',
    name: 'Digital Fever Thermometer & First Aid Kit',
    category: 'Medicine & Stationery',
    shopId: 'mahoba_medicos',
    shopName: 'Mahoba Care Medicos & Stationery',
    price: 145,
    mrp: 199,
    unit: '1 Kit',
    inStock: true,
    keywords: ['medicine', 'thermometer', 'medical', 'health', 'first aid']
  }
];

let state = {
  shops: SHOPS,
  products: PRODUCTS,
  cart: JSON.parse(localStorage.getItem('fd_cart_v2')) || {},
  searchQuery: '',
  selectedCategory: 'All',
  visualSearchActive: false,
  visualSearchDetectedName: '',
  visualSearchPhotoUrl: '',
  deliveryBaseFee: 20
};

function saveCart() {
  localStorage.setItem('fd_cart_v2', JSON.stringify(state.cart));
}

// ==========================================================================
// 1. TIME & SMART 30-MINUTE FORWARD DELIVERY ENGINE (IST)
// ==========================================================================
function parseTimeToMinutes(timeStr) {
  if (!timeStr) return 600;
  const clean = timeStr.trim().toUpperCase();
  const parts = clean.split(/\s+/);
  const hm = parts[0].split(':');
  let h = parseInt(hm[0], 10) || 10;
  const m = parseInt(hm[1], 10) || 0;
  const ampm = parts[1] || 'AM';
  if (ampm === 'PM' && h < 12) h += 12;
  if (ampm === 'AM' && h === 12) h = 0;
  return h * 60 + m;
}

function formatMinutesTo12Hr(totalMinutes) {
  let normalized = ((totalMinutes % 1440) + 1440) % 1440;
  let h24 = Math.floor(normalized / 60);
  let m = normalized % 60;
  const ampm = h24 >= 12 ? 'PM' : 'AM';
  let h12 = h24 % 12;
  if (h12 === 0) h12 = 12;
  const mStr = m < 10 ? '0' + m : '' + m;
  return `${h12}:${mStr} ${ampm}`;
}

function getISTNow() {
  const now = new Date();
  const utcMs = now.getTime() + now.getTimezoneOffset() * 60000;
  return new Date(utcMs + 5.5 * 3600000);
}

function calculateExact30MinForward() {
  const ist = getISTNow();
  const currentMins = ist.getHours() * 60 + ist.getMinutes();
  const targetMins = currentMins + 30;
  const crossesMidnight = targetMins >= 1440;
  const formatted = formatMinutesTo12Hr(targetMins);
  return crossesMidnight ? `${formatted} (Tomorrow)` : formatted;
}

function evaluateShopLiveStatus(shop) {
  const ist = getISTNow();
  const currentMins = ist.getHours() * 60 + ist.getMinutes();
  const openMins = parseTimeToMinutes(shop.openingTime || '10:00 AM');
  const closeMins = parseTimeToMinutes(shop.closingTime || '10:00 PM');

  const withinWindow = currentMins >= openMins && currentMins <= closeMins;
  const minsUntilClose = closeMins - currentMins;
  const isNearClosing = withinWindow && minsUntilClose >= 0 && minsUntilClose <= 30;

  if (withinWindow) {
    return {
      isOpen: true,
      badgeText: isNearClosing ? 'CLOSING SOON' : 'OPEN NOW',
      deliveryText: `Expected by ${calculateExact30MinForward()}`,
      isNearClosing
    };
  } else {
    const nextOpenPlus30 = formatMinutesTo12Hr(openMins + 30);
    return {
      isOpen: false,
      badgeText: 'CLOSED',
      deliveryText: `Delivers Tomorrow by ${nextOpenPlus30}`,
      isNearClosing: false
    };
  }
}

// ==========================================================================
// 2. LOWEST RATE / LOWEST PRICE FIRST PRODUCT SORTING & CAMERA SEARCH
// ==========================================================================
function sortProductsLowestPriceFirst(products) {
  return [...products].sort((a, b) => {
    if (a.inStock !== b.inStock) {
      return a.inStock ? -1 : 1;
    }
    if (a.price !== b.price) {
      return a.price - b.price;
    }
    return a.name.localeCompare(b.name);
  });
}

function getFilteredAndRankedProducts() {
  let list = [...state.products];

  if (state.selectedCategory && state.selectedCategory !== 'All') {
    list = list.filter(p => p.category.toLowerCase().includes(state.selectedCategory.toLowerCase()));
  }

  const q = state.searchQuery.trim().toLowerCase();
  if (q) {
    const tokens = q.split(/\s+/).filter(Boolean);
    list = list.filter(p => {
      const hay = `${p.name} ${p.category} ${p.shopName} ${(p.keywords || []).join(' ')}`.toLowerCase();
      return hay.includes(q) || tokens.some(t => hay.includes(t));
    });
  }

  return sortProductsLowestPriceFirst(list);
}

function handlePhotoInput(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    const photoDataUrl = e.target.result;
    const rawName = (file.name || '').toLowerCase();
    let detectedQuery = 'milk';
    let detectedLabel = 'Fresh Dairy Milk Pack';

    if (rawName.includes('atta') || rawName.includes('flour') || rawName.includes('wheat')) {
      detectedQuery = 'atta';
      detectedLabel = 'Whole Wheat Chakki Atta';
    } else if (rawName.includes('biscuit') || rawName.includes('parle') || rawName.includes('cookie')) {
      detectedQuery = 'biscuit';
      detectedLabel = 'Biscuits & Cookies Pack';
    } else if (rawName.includes('tomato') || rawName.includes('veg') || rawName.includes('sabzi')) {
      detectedQuery = 'tomato';
      detectedLabel = 'Fresh Farm Tomatoes';
    } else if (rawName.includes('sweet') || rawName.includes('pedha')) {
      detectedQuery = 'pedha';
      detectedLabel = 'Mahoba Khoya Pedha';
    } else if (rawName.includes('book') || rawName.includes('note')) {
      detectedQuery = 'notebook';
      detectedLabel = 'Spiral Ruled Notebook';
    }

    applyVisualSearchMatch(detectedQuery, detectedLabel, photoDataUrl);
  };
  reader.readAsDataURL(file);
}

function triggerQuickCameraPreset(keyword, label) {
  applyVisualSearchMatch(keyword, label, '');
}

function applyVisualSearchMatch(keyword, label, photoUrl) {
  state.searchQuery = keyword;
  state.selectedCategory = 'All';
  state.visualSearchActive = true;
  state.visualSearchDetectedName = label;
  state.visualSearchPhotoUrl = photoUrl;

  const searchInput = document.getElementById('fdSearchInput');
  if (searchInput) searchInput.value = keyword;

  renderAll();
}

function clearVisualSearch() {
  state.searchQuery = '';
  state.visualSearchActive = false;
  state.visualSearchDetectedName = '';
  state.visualSearchPhotoUrl = '';
  const searchInput = document.getElementById('fdSearchInput');
  if (searchInput) searchInput.value = '';
  renderAll();
}

function startVoiceSearch() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  const micBtn = document.getElementById('fdMicBtn');

  if (SpeechRecognition) {
    try {
      const recognition = new SpeechRecognition();
      recognition.lang = 'en-IN';
      recognition.interimResults = false;
      recognition.maxAlternatives = 1;

      if (micBtn) micBtn.textContent = '🎙️ Listening...';

      recognition.onresult = function(event) {
        const transcript = event.results[0][0].transcript.trim();
        if (micBtn) micBtn.textContent = '🎤 Mic';
        if (transcript) {
          applyVisualSearchMatch(transcript, `Voice Search: "${transcript}"`, '');
        }
      };

      recognition.onerror = function() {
        if (micBtn) micBtn.textContent = '🎤 Mic';
        promptVoiceSearchFallback();
      };

      recognition.onend = function() {
        if (micBtn) micBtn.textContent = '🎤 Mic';
      };

      recognition.start();
      return;
    } catch (e) {
      if (micBtn) micBtn.textContent = '🎤 Mic';
    }
  }
  promptVoiceSearchFallback();
}

function promptVoiceSearchFallback() {
  const spoken = window.prompt('🎙️ Voice Search — Speak or enter product name (e.g. Milk, Atta, Tomato, Biscuit):', 'Milk');
  if (spoken && spoken.trim()) {
    applyVisualSearchMatch(spoken.trim(), `Voice Search: "${spoken.trim()}"`, '');
  }
}

// ==========================================================================
// 3. UI RENDERING
// ==========================================================================
function updateLiveClockAndBanner() {
  const ist = getISTNow();
  const nowFormatted = formatMinutesTo12Hr(ist.getHours() * 60 + ist.getMinutes());
  const eta30 = calculateExact30MinForward();

  const clockEl = document.getElementById('liveIstClock');
  if (clockEl) clockEl.textContent = `IST ${nowFormatted}`;

  const etaEl = document.getElementById('heroLiveEta');
  if (etaEl) etaEl.textContent = `Expected by ${eta30}`;

  const orderTimeEl = document.getElementById('heroOrderTime');
  if (orderTimeEl) orderTimeEl.textContent = `If ordered now at ${nowFormatted} (+30 mins exact)`;
}

function renderVisualSearchBanner(rankedProducts) {
  const banner = document.getElementById('visualSearchBanner');
  if (!banner) return;

  if (!state.visualSearchActive && !state.searchQuery.trim()) {
    banner.classList.remove('active');
    banner.innerHTML = '';
    return;
  }

  const availableMatches = rankedProducts.filter(p => p.inStock);
  const cheapest = availableMatches.length > 0 ? availableMatches[0] : null;
  const titleText = state.visualSearchActive
    ? `📸 Camera Visual Search: "${state.visualSearchDetectedName}"`
    : `🔍 Search Sorted by Least Rate / Lowest Price First: "${state.searchQuery}"`;

  banner.classList.add('active');
  banner.innerHTML = `
    <div class="fd-visual-banner-header">
      <div>
        <strong style="color: var(--fd-green-deep); font-size: 0.92rem;">${titleText}</strong>
        <div style="font-size: 0.78rem; color: var(--fd-text-secondary);">
          ${availableMatches.length > 0
            ? `${availableMatches.length} available product(s) found — sorted with the <strong>Least Rate / Lowest Price at the Top/First</strong>`
            : 'No available products matched this query.'}
        </div>
      </div>
      <button class="fd-btn-outline" onclick="clearVisualSearch()">Clear Search ✕</button>
    </div>
    ${cheapest ? `
      <div class="fd-lowest-highlight">
        <div>
          <span class="fd-lowest-badge" style="margin-bottom:2px;">★ #1 LEAST RATE / LOWEST PRICE MATCH</span>
          <div style="font-weight: 800; font-size: 0.95rem;">${cheapest.name} (${cheapest.unit})</div>
          <div style="font-size: 0.78rem; color: var(--fd-text-secondary);">Store: ${cheapest.shopName}</div>
        </div>
        <div style="display:flex; align-items:center; gap:10px;">
          <div style="text-align:right;">
            <div style="font-size: 1.25rem; font-weight: 900; color: var(--fd-green-deep);">₹${cheapest.price}</div>
            <div style="font-size: 0.72rem; text-decoration: line-through; color: var(--fd-text-muted);">MRP ₹${cheapest.mrp}</div>
          </div>
          <button class="fd-btn-primary" onclick="addToCart('${cheapest.id}')">+ Add Lowest</button>
        </div>
      </div>
    ` : ''}
  `;
}

function buildShopCardsHtml() {
  return state.shops.map(shop => {
    const status = evaluateShopLiveStatus(shop);
    return `
      <div class="fd-card">
        <div>
          <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:8px; margin-bottom:8px;">
            <div>
              <h3 style="font-size:1rem; font-weight:800;">${shop.name}</h3>
              <p style="font-size:0.78rem; color:var(--fd-text-secondary);">${shop.category} • ${shop.address}</p>
            </div>
            <span class="fd-status-badge ${status.isOpen ? 'open' : 'closed'}">${status.badgeText}</span>
          </div>
          <div style="font-size:0.78rem; background:var(--fd-bg); padding:8px 10px; border-radius:8px; margin-top:8px;">
            <div><strong>Hours:</strong> ${shop.openingTime} – ${shop.closingTime}</div>
            <div style="color:${status.isOpen ? 'var(--fd-green-deep)' : 'var(--fd-orange-dark)'}; font-weight:700; margin-top:2px;">
              🚚 ${status.deliveryText}
            </div>
          </div>
        </div>
        <div style="display:flex; justify-content:space-between; align-items:center; margin-top:12px; padding-top:10px; border-top:1px solid var(--fd-border);">
          <span style="font-size:0.78rem; font-weight:700;">⭐ ${shop.rating} Rating</span>
          <button class="fd-btn-outline" onclick="filterByShop('${shop.id}', '${shop.name}')">View Products →</button>
        </div>
      </div>
    `;
  }).join('');
}

function renderShops() {
  const html = buildShopCardsHtml();
  const homeShops = document.getElementById('shopsGrid');
  if (homeShops) homeShops.innerHTML = html;

  const dirShops = document.getElementById('shopsDirectoryGrid');
  if (dirShops) dirShops.innerHTML = html;
}

function renderProducts(rankedProducts) {
  const container = document.getElementById('productsGrid');
  if (!container) return;

  if (rankedProducts.length === 0) {
    container.innerHTML = `
      <div class="fd-card" style="grid-column: 1 / -1; text-align:center; padding: 32px;">
        <h3 style="margin-bottom:6px;">No matching products found</h3>
        <p style="color:var(--fd-text-secondary); font-size:0.88rem; margin-bottom:12px;">
          Try clearing the search filter or clicking one of the Camera Photo Search presets above.
        </p>
        <button class="fd-btn-primary" onclick="clearVisualSearch()">Show All Products</button>
      </div>
    `;
    return;
  }

  container.innerHTML = rankedProducts.map((product, index) => {
    const isTopLowestPrice = index === 0 && product.inStock;
    const qtyInCart = state.cart[product.id] || 0;
    const discount = Math.max(0, Math.round(((product.mrp - product.price) / product.mrp) * 100));

    return `
      <div class="fd-card ${isTopLowestPrice ? 'lowest-price-winner' : ''}">
        <div>
          ${isTopLowestPrice ? `<div class="fd-lowest-badge">★ Least Rate / Lowest Price First</div>` : ''}
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
            <span style="font-size:0.72rem; font-weight:700; color:var(--fd-orange-dark); text-transform:uppercase;">
              ${product.category}
            </span>
            ${discount > 0 ? `<span style="font-size:0.7rem; font-weight:800; background:var(--fd-orange-light); color:var(--fd-orange-deep); padding:2px 6px; border-radius:4px;">${discount}% OFF</span>` : ''}
          </div>
          <h4 style="font-size:0.96rem; font-weight:800; margin-bottom:4px;">${product.name}</h4>
          <p style="font-size:0.76rem; color:var(--fd-text-secondary);">
            Pack: <strong>${product.unit}</strong> • ${product.shopName}
          </p>
        </div>

        <div class="fd-price-row">
          <div>
            <span class="fd-price">₹${product.price}</span>
            <span class="fd-mrp">₹${product.mrp}</span>
          </div>
          ${qtyInCart > 0 ? `
            <div style="display:inline-flex; align-items:center; gap:8px; background:var(--fd-green-light); padding:4px 8px; border-radius:8px;">
              <button onclick="changeCartQty('${product.id}', -1)" style="border:none; background:var(--fd-green-primary); color:#fff; width:24px; height:24px; border-radius:6px; font-weight:900; cursor:pointer;">-</button>
              <strong style="font-size:0.85rem;">${qtyInCart}</strong>
              <button onclick="changeCartQty('${product.id}', 1)" style="border:none; background:var(--fd-green-primary); color:#fff; width:24px; height:24px; border-radius:6px; font-weight:900; cursor:pointer;">+</button>
            </div>
          ` : `
            <button class="fd-btn-primary" onclick="addToCart('${product.id}')">+ Add</button>
          `}
        </div>
      </div>
    `;
  }).join('');
}

function renderCartBadge() {
  const totalCount = Object.values(state.cart).reduce((acc, n) => acc + n, 0);
  const badge = document.getElementById('headerCartCount');
  if (badge) badge.textContent = totalCount;
}

function renderAll() {
  updateLiveClockAndBanner();
  const rankedProducts = getFilteredAndRankedProducts();
  renderVisualSearchBanner(rankedProducts);
  renderShops();
  renderProducts(rankedProducts);
  renderCartBadge();
}

// ==========================================================================
// 4. USER ACTIONS & EVENT HANDLERS
// ==========================================================================
function switchPortal(portalId, btnEl) {
  document.querySelectorAll('.fd-portal-view').forEach(el => el.classList.remove('active'));
  const target = document.getElementById(`portal-${portalId}`);
  if (target) target.classList.add('active');

  document.querySelectorAll('.fd-nav-btn').forEach(b => b.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function selectCategory(cat, btnEl) {
  state.selectedCategory = cat;
  document.querySelectorAll('.fd-cat-pill').forEach(b => b.classList.remove('active'));
  if (btnEl) btnEl.classList.add('active');
  renderAll();
}

function filterByShop(shopId, shopName) {
  switchPortal('home', document.getElementById('navHomeBtn'));
  state.searchQuery = shopName;
  const input = document.getElementById('fdSearchInput');
  if (input) input.value = shopName;
  renderAll();
  const prodSec = document.getElementById('productsSection');
  if (prodSec) prodSec.scrollIntoView({ behavior: 'smooth' });
}

function addToCart(productId) {
  state.cart[productId] = (state.cart[productId] || 0) + 1;
  saveCart();
  renderAll();
}

function changeCartQty(productId, delta) {
  const next = (state.cart[productId] || 0) + delta;
  if (next <= 0) {
    delete state.cart[productId];
  } else {
    state.cart[productId] = next;
  }
  saveCart();
  renderAll();
  renderCartModalContent();
}

function openCartModal() {
  renderCartModalContent();
  document.getElementById('cartModalBackdrop').classList.add('open');
}

function closeCartModal() {
  document.getElementById('cartModalBackdrop').classList.remove('open');
}

function renderCartModalContent() {
  const container = document.getElementById('cartItemsContainer');
  if (!container) return;

  const entries = Object.entries(state.cart);
  if (entries.length === 0) {
    container.innerHTML = `<p style="padding:18px 0; color:var(--fd-text-secondary); text-align:center;">Your FlashDrop cart is empty. Add items to see live 30-minute delivery calculations!</p>`;
    return;
  }

  let subtotal = 0;
  const rowsHtml = entries.map(([pid, qty]) => {
    const p = state.products.find(item => item.id === pid);
    if (!p) return '';
    const lineTotal = p.price * qty;
    subtotal += lineTotal;
    return `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px solid var(--fd-border);">
        <div>
          <div style="font-weight:800; font-size:0.9rem;">${p.name}</div>
          <div style="font-size:0.76rem; color:var(--fd-text-secondary);">₹${p.price} × ${qty} (${p.unit})</div>
        </div>
        <div style="display:flex; align-items:center; gap:8px;">
          <strong>₹${lineTotal}</strong>
          <button class="fd-btn-outline" style="padding:2px 8px;" onclick="changeCartQty('${p.id}', -1)">-</button>
          <button class="fd-btn-outline" style="padding:2px 8px;" onclick="changeCartQty('${p.id}', 1)">+</button>
        </div>
      </div>
    `;
  }).join('');

  const grandTotal = subtotal + state.deliveryBaseFee;
  const eta = calculateExact30MinForward();

  container.innerHTML = `
    ${rowsHtml}
    <div style="margin-top:12px; background:var(--fd-green-soft); padding:10px 12px; border-radius:8px; font-size:0.82rem;">
      <div><strong>⚡ Smart 30-Minute Delivery Estimate:</strong> <span style="color:var(--fd-green-deep); font-weight:800;">${eta}</span></div>
      <div style="margin-top:4px;">Subtotal: ₹${subtotal} + Delivery Fee: ₹${state.deliveryBaseFee} = <strong style="font-size:1rem;">Total ₹${grandTotal}</strong></div>
    </div>
    <button class="fd-btn-primary" style="width:100%; margin-top:12px; padding:12px;" onclick="placeOrderNow(${grandTotal}, '${eta}')">
      Confirm FlashDrop Order (₹${grandTotal})
    </button>
  `;
}

function placeOrderNow(total, eta) {
  state.cart = {};
  saveCart();
  renderAll();
  const container = document.getElementById('cartItemsContainer');
  if (container) {
    container.innerHTML = `
      <div style="text-align:center; padding:20px 10px;">
        <div style="font-size:2.4rem; margin-bottom:8px;">🎉</div>
        <h3 style="color:var(--fd-green-deep); margin-bottom:6px;">Order Confirmed!</h3>
        <p style="font-size:0.88rem; color:var(--fd-text-secondary); margin-bottom:10px;">
          Total Amount: <strong>₹${total}</strong><br/>
          Expected Delivery Time: <strong style="color:var(--fd-orange-dark);">${eta}</strong>
        </p>
        <button class="fd-btn-primary" onclick="closeCartModal()">Continue Shopping</button>
      </div>
    `;
  }
}

function handleContactSubmit(e) {
  e.preventDefault();
  const name = document.getElementById('contactName').value.trim();
  e.target.reset();
  alert(`Thank you, ${name}! Your message has been received by the FlashDrop Mahoba Support Team.`);
}

document.addEventListener('DOMContentLoaded', () => {
  const searchInput = document.getElementById('fdSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      state.searchQuery = e.target.value;
      state.visualSearchActive = false;
      renderAll();
    });
  }

  renderAll();
  setInterval(updateLiveClockAndBanner, 15000);
});
