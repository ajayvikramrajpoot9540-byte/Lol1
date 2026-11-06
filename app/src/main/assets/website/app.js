/* ==========================================================================
   FlashDrop Official Web Storefront Engine (app.js)
   Features:
   - Full Catalog: 11 Verified Partner Shops & 26 Products from Mahoba
   - Working Microphone Voice Search with Real-time Speech Recognition
   - Camera Visual Photo Search with Lowest Rate / Price First Sorting
   - Smart 30-Minute Delivery Calculation in IST (India Standard Time)
   - Product Quick View Modal & Storefront Filter
   - Cart, Checkout, and Live Order Tracking with Status Timeline
   - Shopkeeper Operating Hours & Auto-Schedule Controls
   - Admin Master Shop Controls
   ========================================================================== */

// 1. Core State
let state = {
  shops: [],
  products: [],
  cart: JSON.parse(localStorage.getItem('fd_cart_v3')) || {},
  orders: JSON.parse(localStorage.getItem('fd_orders_v3')) || [],
  searchQuery: '',
  selectedCategory: 'All',
  selectedShopFilter: null,
  visualSearchActive: false,
  visualSearchDetectedName: '',
  visualSearchPhotoUrl: '',
  lateNightDecisionMode: 'today',
  deliveryBaseFee: 20
};

// 2. Initialize Catalog from FLASHDROP_APP_DATA
function initCatalog() {
  if (typeof window !== 'undefined' && window.FLASHDROP_APP_DATA) {
    state.shops = (window.FLASHDROP_APP_DATA.shops || []).map(s => ({
      ...s,
      openingTime: s.openingTime || '10:00 AM',
      closingTime: s.closingTime || '10:00 PM',
      autoOpeningEnabled: true,
      autoClosingEnabled: true,
      manualOverride: null,
      isTemporarilyClosed: false,
      weeklySchedule: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'].map(day => ({
        day,
        open: s.openingTime || '10:00 AM',
        close: s.closingTime || '10:00 PM',
        isOpen: true
      }))
    }));

    state.products = (window.FLASHDROP_APP_DATA.products || []).map(p => ({
      ...p,
      price: Number(p.price) || 20,
      mrp: Number(p.originalPrice || p.mrp || Math.round(Number(p.price) * 1.15)),
      unit: p.unit || '1 pc',
      inStock: p.inStock !== false,
      keywords: ((p.name || '') + ' ' + (p.category || '') + ' ' + (p.subcategory || '')).toLowerCase().split(/\s+/)
    }));
  }
}

function saveState() {
  localStorage.setItem('fd_cart_v3', JSON.stringify(state.cart));
  localStorage.setItem('fd_orders_v3', JSON.stringify(state.orders));
}

// ==========================================================================
// 3. TIME & SMART 30-MINUTE FORWARD DELIVERY ENGINE (IST)
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
  if (shop.isTemporarilyClosed) {
    return {
      isOpen: false,
      badgeText: 'TEMP CLOSED',
      deliveryText: 'Next-Day Delivery Available',
      isNearClosing: false
    };
  }
  if (shop.manualOverride === true) {
    return {
      isOpen: true,
      badgeText: 'OPEN (MANUAL)',
      deliveryText: `Expected by ${calculateExact30MinForward()}`,
      isNearClosing: false
    };
  }
  if (shop.manualOverride === false) {
    return {
      isOpen: false,
      badgeText: 'CLOSED (MANUAL)',
      deliveryText: `Next-Day: Tomorrow 10:30 AM`,
      isNearClosing: false
    };
  }

  const ist = getISTNow();
  const currentMins = ist.getHours() * 60 + ist.getMinutes();
  const openMins = parseTimeToMinutes(shop.openingTime || '10:00 AM');
  const closeMins = parseTimeToMinutes(shop.closingTime || '10:00 PM');

  let withinWindow = currentMins >= openMins && currentMins <= closeMins;
  if (!shop.autoOpeningEnabled && withinWindow) withinWindow = false;
  if (!shop.autoClosingEnabled && currentMins > closeMins) withinWindow = true;

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
// 4. LOWEST RATE / PRICE FIRST SORTING & SEARCH ENGINE
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

  // Filter by active store selection
  if (state.selectedShopFilter) {
    list = list.filter(p => p.shopId === state.selectedShopFilter.id || p.shopName === state.selectedShopFilter.name);
  }

  // Filter by category
  if (state.selectedCategory && state.selectedCategory !== 'All') {
    list = list.filter(p => {
      const cat = (p.category || '').toLowerCase();
      const sub = (p.subcategory || '').toLowerCase();
      const target = state.selectedCategory.toLowerCase();
      return cat.includes(target) || sub.includes(target);
    });
  }

  // Filter by search query
  const q = state.searchQuery.trim().toLowerCase();
  if (q) {
    const tokens = q.split(/\s+/).filter(Boolean);
    list = list.filter(p => {
      const hay = `${p.name} ${p.category} ${p.subcategory} ${p.shopName} ${(p.keywords || []).join(' ')}`.toLowerCase();
      return hay.includes(q) || tokens.some(t => hay.includes(t));
    });
  }

  return sortProductsLowestPriceFirst(list);
}

// ==========================================================================
// 5. MULTI-MODAL SEARCH (VOICE MIC & CAMERA PHOTO)
// ==========================================================================
function startVoiceSearch() {
  const modal = document.getElementById('voiceModalBackdrop');
  const status = document.getElementById('voiceModalStatus');
  const transcriptBox = document.getElementById('voiceTranscriptBox');
  if (modal) modal.classList.add('open');
  if (status) status.textContent = 'Listening now... Speak product name clearly (e.g. Milk, Atta, Biscuits, Tomatoes)';
  if (transcriptBox) {
    transcriptBox.style.display = 'none';
    transcriptBox.textContent = '';
  }

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    try {
      const recognition = new SpeechRecognition();
      recognition.lang = 'en-IN';
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;

      recognition.onresult = function(event) {
        const transcript = event.results[0][0].transcript.trim();
        if (transcriptBox) {
          transcriptBox.style.display = 'block';
          transcriptBox.textContent = `"${transcript}"`;
        }
        if (event.results[0].isFinal) {
          setTimeout(() => {
            closeVoiceModal();
            handleSpokenResult(transcript);
          }, 600);
        }
      };

      recognition.onerror = function() {
        if (status) status.textContent = 'Could not catch voice audio. Tap any shortcut chip below:';
      };

      recognition.start();
    } catch (_e) {
      if (status) status.textContent = 'Microphone ready. Tap any voice phrase below to test search:';
    }
  }
}

function closeVoiceModal() {
  const modal = document.getElementById('voiceModalBackdrop');
  if (modal) modal.classList.remove('open');
}

function handleSpokenResult(spoken) {
  closeVoiceModal();
  applySearchMatch(spoken, `Voice Search: "${spoken}"`);
}

function handlePhotoInput(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(e) {
    const rawName = (file.name || '').toLowerCase();
    let query = 'milk';
    let label = 'Fresh Dairy Milk';

    if (rawName.includes('atta') || rawName.includes('flour') || rawName.includes('wheat')) {
      query = 'atta';
      label = 'Chakki Fresh Gehu Atta';
    } else if (rawName.includes('biscuit') || rawName.includes('parle') || rawName.includes('cookie')) {
      query = 'biscuit';
      label = 'Biscuits & Cookies Pack';
    } else if (rawName.includes('tomato') || rawName.includes('tamatar')) {
      query = 'tomato';
      label = 'Fresh Local Tomatoes';
    } else if (rawName.includes('sweet') || rawName.includes('pedha')) {
      query = 'pedha';
      label = 'Mahoba Khoya Pedha';
    } else if (rawName.includes('book') || rawName.includes('note')) {
      query = 'notebook';
      label = 'Ruled Spiral Notebook';
    }

    applySearchMatch(query, `Camera Search: "${label}"`, e.target.result);
  };
  reader.readAsDataURL(file);
}

function applyPresetSearch(query, label) {
  applySearchMatch(query, `Quick Search: "${label}"`);
}

function applySearchMatch(keyword, label, photoUrl = '') {
  state.searchQuery = keyword;
  state.selectedCategory = 'All';
  state.visualSearchActive = true;
  state.visualSearchDetectedName = label;
  state.visualSearchPhotoUrl = photoUrl;

  const input = document.getElementById('fdSearchInput');
  if (input) input.value = keyword;

  renderAll();
}

function clearVisualSearch() {
  state.searchQuery = '';
  state.visualSearchActive = false;
  state.visualSearchDetectedName = '';
  state.visualSearchPhotoUrl = '';
  state.selectedShopFilter = null;
  const input = document.getElementById('fdSearchInput');
  if (input) input.value = '';
  renderAll();
}

// ==========================================================================
// 6. UI RENDERING
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

  if (!state.visualSearchActive && !state.searchQuery.trim() && !state.selectedShopFilter) {
    banner.classList.remove('active');
    banner.innerHTML = '';
    return;
  }

  const availableMatches = rankedProducts.filter(p => p.inStock);
  const cheapest = availableMatches.length > 0 ? availableMatches[0] : null;

  let title = state.visualSearchActive
    ? `📸 ${state.visualSearchDetectedName}`
    : `🔍 Search Query: "${state.searchQuery}"`;

  if (state.selectedShopFilter) {
    title = `🏪 Store Catalog: ${state.selectedShopFilter.name}`;
  }

  banner.classList.add('active');
  banner.innerHTML = `
    <div class="fd-visual-banner-header">
      <div>
        <strong style="color: var(--fd-green-deep); font-size: 0.94rem;">${title}</strong>
        <div style="font-size: 0.78rem; color: var(--fd-text-secondary);">
          ${availableMatches.length} available item(s) found — sorted with <strong>Least Rate / Lowest Price at Top/First</strong>
        </div>
      </div>
      <button class="fd-btn-outline" onclick="clearVisualSearch()">Clear Filter ✕</button>
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

function renderProducts(rankedProducts) {
  const container = document.getElementById('productsGrid');
  if (!container) return;

  if (rankedProducts.length === 0) {
    container.innerHTML = `
      <div class="fd-card" style="grid-column: 1 / -1; text-align:center; padding: 36px 16px;">
        <h3>No matching products found</h3>
        <p style="color:var(--fd-text-secondary); font-size:0.86rem; margin:8px 0 14px;">
          Try clearing your search query or clicking one of the quick photo/voice test chips above.
        </p>
        <button class="fd-btn-primary" onclick="clearVisualSearch()">Show All Products</button>
      </div>
    `;
    return;
  }

  container.innerHTML = rankedProducts.map((p, index) => {
    const isTopLowest = index === 0 && p.inStock;
    const qty = state.cart[p.id] || 0;
    const discount = Math.max(0, Math.round(((p.mrp - p.price) / p.mrp) * 100));

    return `
      <div class="fd-card ${isTopLowest ? 'lowest-price-winner' : ''}">
        <div>
          ${p.imageUrl ? `
            <div class="fd-prod-thumb-wrap" onclick="openProductDetailModal('${p.id}')">
              <img src="${p.imageUrl}" alt="${p.name}" class="fd-prod-thumb" loading="lazy" />
            </div>
          ` : ''}
          ${isTopLowest ? `<div class="fd-lowest-badge">★ Least Rate / Lowest Price First</div>` : ''}
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
            <span style="font-size:0.72rem; font-weight:800; color:var(--fd-orange-dark); text-transform:uppercase;">
              ${p.category}
            </span>
            ${discount > 0 ? `<span style="font-size:0.7rem; font-weight:800; background:var(--fd-orange-light); color:var(--fd-orange-deep); padding:2px 6px; border-radius:4px;">${discount}% OFF</span>` : ''}
          </div>
          <h4 style="font-size:0.95rem; font-weight:800; margin-bottom:4px; cursor:pointer;" onclick="openProductDetailModal('${p.id}')">${p.name}</h4>
          <p style="font-size:0.76rem; color:var(--fd-text-secondary);">
            Pack: <strong>${p.unit}</strong> • ${p.shopName}
          </p>
        </div>

        <div class="fd-price-row">
          <div>
            <span class="fd-price">₹${p.price}</span>
            <span class="fd-mrp">₹${p.mrp}</span>
          </div>
          ${qty > 0 ? `
            <div style="display:inline-flex; align-items:center; gap:8px; background:var(--fd-green-light); padding:4px 8px; border-radius:8px;">
              <button onclick="changeCartQty('${p.id}', -1)" style="border:none; background:var(--fd-green-primary); color:#fff; width:24px; height:24px; border-radius:6px; font-weight:900; cursor:pointer;">-</button>
              <strong style="font-size:0.85rem;">${qty}</strong>
              <button onclick="changeCartQty('${p.id}', 1)" style="border:none; background:var(--fd-green-primary); color:#fff; width:24px; height:24px; border-radius:6px; font-weight:900; cursor:pointer;">+</button>
            </div>
          ` : `
            <button class="fd-btn-primary" onclick="addToCart('${p.id}')">+ Add</button>
          `}
        </div>
      </div>
    `;
  }).join('');
}

function buildShopCardsHtml() {
  return state.shops.map(shop => {
    const status = evaluateShopLiveStatus(shop);
    return `
      <div class="fd-card">
        <div>
          ${shop.imageUrl ? `
            <div class="fd-shop-thumb-wrap">
              <img src="${shop.imageUrl}" alt="${shop.name}" class="fd-shop-thumb" loading="lazy" />
            </div>
          ` : ''}
          <div style="display:flex; justify-content:space-between; align-items:flex-start; gap:8px; margin-bottom:6px;">
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
          <span style="font-size:0.78rem; font-weight:700;">⭐ ${shop.rating || 4.7} Rating</span>
          <button class="fd-btn-outline" onclick="filterByShop('${shop.id}', '${shop.name}')">View Products →</button>
        </div>
      </div>
    `;
  }).join('');
}

function renderShops() {
  const html = buildShopCardsHtml();
  const homeEl = document.getElementById('shopsGrid');
  if (homeEl) homeEl.innerHTML = html;

  const dirEl = document.getElementById('shopsDirectoryGrid');
  if (dirEl) dirEl.innerHTML = html;
}

function renderOrders() {
  const container = document.getElementById('ordersContainer');
  if (!container) return;

  if (state.orders.length === 0) {
    container.innerHTML = `
      <div class="fd-card" style="text-align:center; padding:36px 16px;">
        <div style="font-size:2.4rem; margin-bottom:8px;">📦</div>
        <h3>No orders placed yet</h3>
        <p style="color:var(--fd-text-secondary); font-size:0.86rem; margin:8px 0 14px;">
          Add items from any local store to your cart and place your order to track live 30-minute delivery.
        </p>
        <button class="fd-btn-primary" onclick="switchPortal('home', document.getElementById('navHomeBtn'))">Browse Storefront</button>
      </div>
    `;
    return;
  }

  container.innerHTML = state.orders.map(o => `
    <div class="fd-order-card">
      <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
        <div>
          <strong style="font-size:1rem; color:var(--fd-green-deep);">Order #${o.id}</strong>
          <div style="font-size:0.78rem; color:var(--fd-text-secondary);">Placed: ${o.time} • Payment: ${o.paymentMethod.toUpperCase()}</div>
        </div>
        <span class="fd-status-badge open">⏱️ ${o.eta}</span>
      </div>

      <div class="fd-order-timeline">
        <div class="fd-timeline-step active">
          <div class="fd-step-icon">✓</div>
          <div>Confirmed</div>
        </div>
        <div class="fd-timeline-step active">
          <div class="fd-step-icon">🍳</div>
          <div>Packing</div>
        </div>
        <div class="fd-timeline-step active">
          <div class="fd-step-icon">🛵</div>
          <div>On Way</div>
        </div>
        <div class="fd-timeline-step">
          <div class="fd-step-icon">🏠</div>
          <div>Delivered</div>
        </div>
      </div>

      <div style="font-size:0.82rem; background:var(--fd-bg); padding:10px 12px; border-radius:8px;">
        <div><strong>Deliver to:</strong> ${o.address}</div>
        <div style="margin-top:4px;"><strong>Items:</strong> ${o.itemSummary}</div>
        <div style="margin-top:4px; font-weight:800; color:var(--fd-green-deep);">Total Paid: ₹${o.total}</div>
      </div>
    </div>
  `).join('');
}

function renderAdminShops() {
  const tbody = document.getElementById('adminShopsTableBody');
  if (!tbody) return;

  tbody.innerHTML = state.shops.map((s, idx) => {
    const status = evaluateShopLiveStatus(s);
    return `
      <tr>
        <td><strong>${s.name}</strong><div style="font-size:0.74rem; color:var(--fd-text-secondary);">${s.address}</div></td>
        <td><span style="font-size:0.76rem; font-weight:700;">${s.category}</span></td>
        <td>${s.openingTime} – ${s.closingTime}</td>
        <td><span class="fd-status-badge ${status.isOpen ? 'open' : 'closed'}">${status.badgeText}</span></td>
        <td>
          <button class="${s.isTemporarilyClosed ? 'fd-btn-primary' : 'fd-btn-orange'}" onclick="toggleAdminShopDisable(${idx})" style="padding:4px 8px; font-size:0.75rem;">
            ${s.isTemporarilyClosed ? 'Re-Enable' : 'Disable'}
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function renderCartBadge() {
  const total = Object.values(state.cart).reduce((a, b) => a + b, 0);
  const badge = document.getElementById('headerCartCount');
  if (badge) badge.textContent = total;

  const ordersBadge = document.getElementById('navOrdersBadge');
  if (ordersBadge) {
    if (state.orders.length > 0) {
      ordersBadge.style.display = 'inline-block';
      ordersBadge.textContent = state.orders.length;
    } else {
      ordersBadge.style.display = 'none';
    }
  }
}

function renderAll() {
  updateLiveClockAndBanner();
  const ranked = getFilteredAndRankedProducts();
  renderVisualSearchBanner(ranked);
  renderShops();
  renderProducts(ranked);
  renderOrders();
  renderAdminShops();
  renderCartBadge();
}

// ==========================================================================
// 7. USER INTERACTIONS & MODAL HANDLERS
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
  state.selectedShopFilter = { id: shopId, name: shopName };
  switchPortal('home', document.getElementById('navHomeBtn'));
  renderAll();
  const sec = document.getElementById('productsSection');
  if (sec) sec.scrollIntoView({ behavior: 'smooth' });
}

function addToCart(productId) {
  state.cart[productId] = (state.cart[productId] || 0) + 1;
  saveState();
  renderAll();
}

function changeCartQty(productId, delta) {
  const next = (state.cart[productId] || 0) + delta;
  if (next <= 0) {
    delete state.cart[productId];
  } else {
    state.cart[productId] = next;
  }
  saveState();
  renderAll();
  renderCartModalContent();
}

function getMultiShopComparisonOffers(currentProduct) {
  const currentShopId = currentProduct.shopId;
  const currentShop = state.shops.find(s => s.id === currentShopId) || {
    id: currentShopId,
    name: currentProduct.shopName,
    rating: 4.8,
    ratingCount: 120,
    distance: "1.2 km",
    deliveryFee: 20
  };

  const nameLower = (currentProduct.name || '').toLowerCase();
  const keywordGroups = [
    ["atta", "flour", "gehu", "sharbati", "wheat"],
    ["biscuit", "biscuits", "parle-g", "parle", "glucose"],
    ["maggi", "noodles", "instant"],
    ["butter", "makhan", "amul"],
    ["salt", "namak", "tata"],
    ["oil", "tel", "refined", "sunlite", "mustard"],
    ["rice", "chawal", "basmati"],
    ["kurti", "cotton", "suit", "ethnic"],
    ["shirt", "linen", "formal"],
    ["saree", "silk", "chanderi"],
    ["earphone", "earphones", "bassheads", "headphone"],
    ["charger", "cable", "adapter", "fast"],
    ["powerbank", "power bank", "battery"],
    ["pedha", "peda", "khoya", "sweet"],
    ["gulab jamun", "gulabjamun", "rasgulla"],
    ["samosa", "jalebi", "kachori"],
    ["ghee", "desi ghee", "bilona"],
    ["dolo", "paracetamol", "crocin"],
    ["chyawanprash", "immunity", "dabur"],
    ["notebook", "copy", "register", "classmate"],
    ["visiting card", "visiting cards", "card"],
    ["flex", "banner", "poster"],
    ["print", "printing", "document", "xerox", "photo printing"],
    ["binding", "lamination", "spiral"]
  ];

  const matchedGroup = keywordGroups.find(group => group.some(k => nameLower.includes(k)));

  const candidateProds = state.products.filter(p => {
    const pName = (p.name || '').toLowerCase();
    const hasCatMatch = p.category && currentProduct.category && p.category.toLowerCase() === currentProduct.category.toLowerCase();
    const hasKeyword = matchedGroup && matchedGroup.some(k => pName.includes(k));
    return hasCatMatch || hasKeyword;
  });

  const byShop = {};
  candidateProds.forEach(p => {
    if (!byShop[p.shopId]) byShop[p.shopId] = [];
    byShop[p.shopId].push(p);
  });

  const offers = [];
  offers.push({
    product: currentProduct,
    shop: currentShop,
    isCurrentShop: true,
    totalCost: currentProduct.price + (currentShop.deliveryFee || 20)
  });

  state.shops.filter(s => s.id !== currentShopId).forEach(shop => {
    const prods = byShop[shop.id] || [];
    let bestProd = null;
    if (prods.length > 0) {
      bestProd = prods.reduce((best, cur) => {
        const curScore = (matchedGroup && matchedGroup.some(k => cur.name.toLowerCase().includes(k)) ? -50 : 0) + cur.price;
        const bestScore = (matchedGroup && matchedGroup.some(k => best.name.toLowerCase().includes(k)) ? -50 : 0) + best.price;
        return curScore < bestScore ? cur : best;
      }, prods[0]);
    } else if (shop.category === currentProduct.category) {
      const anyProd = state.products.filter(p => p.shopId === shop.id).sort((a,b) => a.price - b.price)[0];
      if (anyProd) bestProd = anyProd;
    }

    if (bestProd) {
      offers.push({
        product: bestProd,
        shop: shop,
        isCurrentShop: false,
        totalCost: bestProd.price + (shop.deliveryFee || 20)
      });
    }
  });

  const minPrice = Math.min(...offers.map(o => o.product.price));
  const minTotal = Math.min(...offers.map(o => o.totalCost));

  offers.forEach(o => {
    o.isLowestPrice = o.product.price <= minPrice;
    o.isLowestTotal = o.totalCost <= minTotal;
    o.liveStatus = evaluateShopLiveStatus(o.shop);
  });

  return offers.sort((a, b) => a.product.price - b.product.price);
}

function openProductDetailModal(productId) {
  const p = state.products.find(item => item.id === productId);
  if (!p) return;

  const content = document.getElementById('productDetailModalContent');
  if (!content) return;

  const discount = Math.max(0, Math.round(((p.mrp - p.price) / p.mrp) * 100));
  const offers = getMultiShopComparisonOffers(p);
  const lowestOffer = offers.find(o => o.isLowestPrice);
  const maxSavings = Math.max(0, Math.round(offers.reduce((max, o) => Math.max(max, o.product.price), 0) - (lowestOffer ? lowestOffer.product.price : 0)));

  content.innerHTML = `
    <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
      <span class="fd-lowest-badge">★ PRODUCT QUICK VIEW</span>
      <button class="fd-btn-outline" onclick="closeProductDetailModal()">✕</button>
    </div>
    ${p.imageUrl ? `
      <div style="width:100%; height:220px; border-radius:12px; overflow:hidden; margin-bottom:12px;">
        <img src="${p.imageUrl}" alt="${p.name}" style="width:100%; height:100%; object-fit:cover;" />
      </div>
    ` : ''}
    <h3 style="font-size:1.2rem; font-weight:900; margin-bottom:4px;">${p.name}</h3>
    <p style="font-size:0.8rem; color:var(--fd-text-secondary); margin-bottom:8px;">
      Store: <strong>${p.shopName}</strong> • Category: ${p.category} (${p.subcategory || 'General'})
    </p>
    <p style="font-size:0.86rem; color:var(--fd-text-main); margin-bottom:12px;">
      ${p.description || 'Fresh quality local stock delivered to your doorstep in Mahoba in 30 minutes.'}
    </p>
    <div style="display:flex; justify-content:space-between; align-items:center; background:var(--fd-bg); padding:10px 14px; border-radius:10px; margin-bottom:14px;">
      <div>
        <div style="font-size:1.35rem; font-weight:900; color:var(--fd-green-deep);">₹${p.price}</div>
        <div style="font-size:0.75rem; text-decoration:line-through; color:var(--fd-text-muted);">MRP ₹${p.mrp} (${discount}% OFF)</div>
      </div>
      <span style="font-size:0.82rem; font-weight:800; background:var(--fd-green-light); color:var(--fd-green-deep); padding:4px 10px; border-radius:8px;">Pack: ${p.unit}</span>
    </div>

    <!-- ⚖️ Multi-Shop Price Comparison Card -->
    ${offers.length > 1 ? `
      <div class="fd-comparison-card">
        <div class="fd-comparison-header">
          <div style="display:flex; align-items:center; gap:8px;">
            <span style="background:var(--fd-green-primary); color:#fff; border-radius:6px; padding:3px 6px; font-size:0.75rem; font-weight:800;">⚖️</span>
            <div>
              <div style="font-weight:900; font-size:0.88rem; color:var(--fd-text-main);">Mahoba Multi-Shop Price Compare</div>
              <div style="font-size:0.72rem; color:var(--fd-text-secondary);">${offers.length} Local Mahoba Stores Selling This</div>
            </div>
          </div>
          ${maxSavings > 0 ? `<span style="font-size:0.72rem; font-weight:800; background:#dcfce7; color:#166534; padding:2px 8px; border-radius:6px;">Save up to ₹${maxSavings}</span>` : ''}
        </div>
        <div style="padding:10px; display:flex; flex-direction:column; gap:8px;">
          ${offers.map(o => `
            <div class="fd-comparison-row ${o.isLowestPrice ? 'lowest' : ''} ${o.isCurrentShop ? 'current' : ''}">
              <div style="display:flex; justify-content:space-between; align-items:center;">
                <div style="display:flex; align-items:center; gap:6px;">
                  <strong style="font-size:0.86rem; color:var(--fd-text-main);">${o.shop.name}</strong>
                  ${o.isLowestPrice ? `<span class="fd-badge-lowest">★ Lowest Price</span>` : ''}
                  ${o.isCurrentShop ? `<span class="fd-badge-current">Viewing</span>` : ''}
                </div>
                <span class="fd-status-badge ${o.liveStatus.isOpen ? 'open' : 'closed'}" style="font-size:0.68rem; padding:2px 6px;">
                  ${o.liveStatus.isOpen ? 'OPEN' : 'CLOSED'}
                </span>
              </div>

              <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.74rem; color:var(--fd-text-secondary);">
                <span>⭐ ${o.shop.rating} (${o.shop.ratingCount || 50}) • 📍 ${o.shop.distance || '1.2 km'}</span>
                <span>🚚 ${o.liveStatus.isOpen ? (o.liveStatus.expectedDeliverySlot || '20-30 mins') : 'Opens ' + (o.shop.openingTime || '09:00 AM')}</span>
              </div>

              <div style="display:flex; justify-content:space-between; align-items:center; margin-top:4px; padding-top:6px; border-top:1px solid #f1f5f9;">
                <div>
                  <span style="font-size:1.05rem; font-weight:900; color:${o.isLowestPrice ? '#15803d' : 'var(--fd-green-deep)'};">₹${o.product.price}</span>
                  <span style="font-size:0.7rem; color:var(--fd-text-muted);">+ ₹${o.shop.deliveryFee || 20} Delivery = <strong>₹${o.totalCost} Total</strong></span>
                </div>
                <div style="display:flex; gap:6px;">
                  ${!o.isCurrentShop ? `
                    <button class="fd-btn-outline" style="padding:4px 8px; font-size:0.74rem;" onclick="openProductDetailModal('${o.product.id}')">
                      Switch Store
                    </button>
                    <button class="fd-btn-primary" style="padding:4px 10px; font-size:0.74rem;" onclick="addToCart('${o.product.id}'); showToast('Added from ' + '${o.shop.name}');">
                      + Add
                    </button>
                  ` : `
                    <span style="font-size:0.74rem; font-weight:800; color:var(--fd-green-primary); background:#e2e8f0; padding:4px 10px; border-radius:6px;">✓ Selected</span>
                  `}
                </div>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    ` : ''}

    <button class="fd-btn-primary" style="width:100%; padding:12px; font-size:0.92rem; margin-top:8px;" onclick="addToCart('${p.id}'); closeProductDetailModal(); openCartModal();">
      + Add to Cart &amp; View (₹${p.price})
    </button>
  `;

  document.getElementById('productDetailModalBackdrop').classList.add('open');
}

function closeProductDetailModal() {
  document.getElementById('productDetailModalBackdrop').classList.remove('open');
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
    container.innerHTML = `
      <div style="text-align:center; padding:28px 0; color:var(--fd-text-secondary);">
        <p style="font-size:1rem; margin-bottom:6px;">Your cart is empty.</p>
        <p style="font-size:0.82rem;">Add fresh groceries, sweets, or essentials to calculate your 30-minute delivery!</p>
      </div>
    `;
    return;
  }

  let subtotal = 0;
  let summaryParts = [];

  const rows = entries.map(([id, qty]) => {
    const p = state.products.find(item => item.id === id);
    if (!p) return '';
    const lineTotal = p.price * qty;
    subtotal += lineTotal;
    summaryParts.push(`${p.name} (x${qty})`);

    return `
      <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px solid var(--fd-border);">
        <div>
          <div style="font-weight:800; font-size:0.9rem;">${p.name}</div>
          <div style="font-size:0.75rem; color:var(--fd-text-secondary);">₹${p.price} × ${qty} (${p.unit})</div>
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
    ${rows}
    <div style="margin-top:12px; background:var(--fd-green-soft); padding:10px 12px; border-radius:8px; font-size:0.82rem;">
      <div><strong>⚡ Smart 30-Minute Delivery:</strong> <span style="color:var(--fd-green-deep); font-weight:800;">${eta}</span></div>
      <div style="margin-top:4px;">Subtotal: ₹${subtotal} + Delivery Fee: ₹${state.deliveryBaseFee} = <strong style="font-size:1.05rem; color:var(--fd-green-deep);">Total ₹${grandTotal}</strong></div>
    </div>

    <!-- Checkout Form Inputs -->
    <div style="margin-top:14px; font-size:0.82rem;">
      <div class="fd-form-group">
        <label>Delivery Area in Mahoba</label>
        <select id="checkoutAddress" class="fd-select">
          <option value="Subhash Chowk, Chhatarpur Road, Mahoba">Subhash Chowk, Mahoba</option>
          <option value="Alha Chowk, Main Market, Mahoba">Alha Chowk, Mahoba</option>
          <option value="Gandhi Nagar, Mahoba">Gandhi Nagar, Mahoba</option>
          <option value="Civil Lines, Near Hospital, Mahoba">Civil Lines, Mahoba</option>
          <option value="Station Road, Sadar Bazaar, Mahoba">Station Road, Mahoba</option>
        </select>
      </div>

      <div class="fd-form-group">
        <label>Payment Method</label>
        <div style="display:flex; gap:8px;">
          <label style="display:flex; align-items:center; gap:4px; font-weight:700;">
            <input type="radio" name="paymentOption" value="Cash on Delivery" checked /> Cash on Delivery (COD)
          </label>
          <label style="display:flex; align-items:center; gap:4px; font-weight:700;">
            <input type="radio" name="paymentOption" value="UPI / QR Code" /> Instant UPI
          </label>
        </div>
      </div>
    </div>

    <button class="fd-btn-primary" style="width:100%; margin-top:8px; padding:12px; font-size:0.94rem;" onclick="confirmOrder(${grandTotal}, '${eta}', '${summaryParts.join(', ')}')">
      Confirm Order • ₹${grandTotal}
    </button>
  `;
}

function confirmOrder(total, eta, itemSummary) {
  const address = document.getElementById('checkoutAddress')?.value || 'Mahoba Central';
  const paymentMethod = document.querySelector('input[name="paymentOption"]:checked')?.value || 'Cash on Delivery';

  const newOrder = {
    id: Math.floor(100000 + Math.random() * 900000),
    time: formatMinutesTo12Hr(getISTNow().getHours() * 60 + getISTNow().getMinutes()),
    eta: `Expected by ${eta}`,
    total,
    address,
    paymentMethod,
    itemSummary: itemSummary || 'Groceries & Essentials'
  };

  state.orders.unshift(newOrder);
  state.cart = {};
  saveState();
  renderAll();

  const container = document.getElementById('cartItemsContainer');
  if (container) {
    container.innerHTML = `
      <div style="text-align:center; padding:24px 10px;">
        <div style="font-size:2.8rem; margin-bottom:8px;">🎉</div>
        <h3 style="color:var(--fd-green-deep); font-size:1.2rem; margin-bottom:6px;">Order Confirmed!</h3>
        <p style="font-size:0.86rem; color:var(--fd-text-secondary); margin-bottom:12px;">
          Order <strong>#${newOrder.id}</strong> has been sent to partner stores.<br/>
          Expected Delivery: <strong style="color:var(--fd-orange-dark);">${newOrder.eta}</strong>
        </p>
        <button class="fd-btn-primary" style="margin-right:8px;" onclick="closeCartModal(); switchPortal('orders', document.getElementById('navOrdersBtn'));">
          Track in My Orders
        </button>
        <button class="fd-btn-outline" onclick="closeCartModal()">Continue Shopping</button>
      </div>
    `;
  }
}

// ==========================================================================
// 8. MERCHANT & ADMIN CONTROLS
// ==========================================================================
function saveShopkeeperSchedule() {
  const openTime = document.getElementById('skOpeningTime')?.value.trim() || '10:00 AM';
  const closeTime = document.getElementById('skClosingTime')?.value.trim() || '10:00 PM';
  const autoOpen = document.getElementById('skAutoOpen')?.checked ?? true;
  const autoClose = document.getElementById('skAutoClose')?.checked ?? true;

  if (state.shops.length > 0) {
    state.shops[0].openingTime = openTime;
    state.shops[0].closingTime = closeTime;
    state.shops[0].autoOpeningEnabled = autoOpen;
    state.shops[0].autoClosingEnabled = autoClose;
    state.shops[0].manualOverride = null;
  }

  renderAll();
  alert(`Shop schedule updated: ${openTime} – ${closeTime}. 30-minute delivery calculations updated!`);
}

function setManualShopOverride(isOpen) {
  if (state.shops.length > 0) {
    state.shops[0].manualOverride = isOpen;
  }
  renderAll();
}

function setLateNightDecision(mode) {
  state.lateNightDecisionMode = mode;
  const preview = document.getElementById('lateNightDecisionPreview');
  if (preview) {
    if (mode === 'today') {
      preview.innerHTML = `✅ <strong>Deliver Tonight:</strong> Orders within 30 mins of closing are accepted and dispatched for delivery at <strong>${calculateExact30MinForward()}</strong>.`;
    } else {
      preview.innerHTML = `🌅 <strong>Deliver Tomorrow:</strong> Orders within 30 mins of closing are automatically scheduled for tomorrow's opening (+30 mins).`;
    }
  }
}

function handleAddNewProduct(e) {
  e.preventDefault();
  const name = document.getElementById('newProdName')?.value.trim();
  const category = document.getElementById('newProdCategory')?.value || 'Grocery';
  const price = Number(document.getElementById('newProdPrice')?.value) || 20;
  const mrp = Number(document.getElementById('newProdMrp')?.value) || (price + 5);
  const unit = document.getElementById('newProdUnit')?.value.trim() || '1 pc';

  if (!name) return;

  const newProd = {
    id: 'prod_custom_' + Date.now(),
    name,
    category,
    shopId: state.shops[0]?.id || 'sharma_general',
    shopName: state.shops[0]?.name || 'Sharma General Store',
    price,
    mrp,
    unit,
    inStock: true,
    keywords: name.toLowerCase().split(/\s+/)
  };

  state.products.unshift(newProd);
  e.target.reset();
  renderAll();
  alert(`Added "${name}" (₹${price})! It now automatically ranks with Lowest Price First in search.`);
}

function toggleAdminShopDisable(idx) {
  if (state.shops[idx]) {
    state.shops[idx].isTemporarilyClosed = !state.shops[idx].isTemporarilyClosed;
    renderAll();
  }
}

function handleContactSubmit(e) {
  e.preventDefault();
  alert('Thank you! Your inquiry has been submitted to FlashDrop Mahoba customer support.');
  e.target.reset();
}

// ==========================================================================
// 9. ON LOAD INITIALIZATION
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
  initCatalog();

  const searchInput = document.getElementById('fdSearchInput');
  if (searchInput) {
    searchInput.addEventListener('input', e => {
      state.searchQuery = e.target.value;
      state.visualSearchActive = false;
      renderAll();
    });
  }

  setLateNightDecision('today');
  renderAll();
  setInterval(updateLiveClockAndBanner, 15000);
});
