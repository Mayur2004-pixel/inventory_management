/**
 * SmartStock — Enterprise Inventory Management System Client Application
 * Connects frontend views directly with Spring Boot REST endpoints and Oracle PL/SQL.
 */

// Application State
const state = {
    categories: [],
    suppliers: [],
    products: [],
    currentView: 'dashboard',
    charts: {
        categoryStock: null,
        movementTrend: null
    }
};

// ==============================================================================
// 1. Initializer & Navigation
// ==============================================================================
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    loadLookupData();
    navigateTo('dashboard');

    // Setup form submit handlers
    setupFormHandlers();
});

function initNavigation() {
    const navLinks = document.querySelectorAll('.nav-item-link[data-view]');
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const targetView = link.getAttribute('data-view');
            navigateTo(targetView);

            // On mobile, close sidebar after clicking
            const sidebar = document.getElementById('sidebar');
            if (sidebar && sidebar.classList.contains('show')) {
                sidebar.classList.remove('show');
            }
        });
    });

    const sidebarToggle = document.getElementById('sidebarToggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', () => {
            const sidebar = document.getElementById('sidebar');
            sidebar.classList.toggle('show');
        });
    }
}

function navigateTo(viewId) {
    state.currentView = viewId;

    // Update active nav link
    document.querySelectorAll('.nav-item-link').forEach(l => l.classList.remove('active'));
    const activeLink = document.querySelector(`.nav-item-link[data-view="${viewId}"]`);
    if (activeLink) activeLink.classList.add('active');

    // Update view panes
    document.querySelectorAll('.view-pane').forEach(p => p.classList.remove('active'));
    const activePane = document.getElementById(`view-${viewId}`);
    if (activePane) activePane.classList.add('active');

    // Update page title
    const titles = {
        'dashboard': 'Enterprise Inventory Dashboard',
        'products': 'Product Catalog & Inventory',
        'stock-in': 'Stock In — Receiving & Procurement',
        'stock-out': 'Stock Out — Sales & Issue Dispatch',
        'movements': 'Inventory Transaction History',
        'low-stock': 'Low-Stock & Replenishment Alerts',
        'suppliers': 'Supplier Directory'
    };
    const titleEl = document.getElementById('topbarPageTitle');
    if (titleEl) titleEl.textContent = titles[viewId] || 'Inventory Dashboard';

    // Route-specific data refresh
    switch (viewId) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'products':
            loadProducts();
            break;
        case 'stock-in':
            prepareStockInForm();
            break;
        case 'stock-out':
            prepareStockOutForm();
            break;
        case 'movements':
            loadMovements();
            break;
        case 'low-stock':
            loadLowStockAlerts();
            break;
        case 'suppliers':
            loadSuppliers();
            break;
    }
}

// ==============================================================================
// 2. Lookup Data (Categories & Suppliers)
// ==============================================================================
async function loadLookupData() {
    try {
        const [catRes, supRes] = await Promise.all([
            fetch('/api/categories').then(r => r.json()),
            fetch('/api/suppliers').then(r => r.json())
        ]);

        if (catRes.success) {
            state.categories = catRes.data;
            populateCategoryDropdowns();
        }

        if (supRes.success) {
            state.suppliers = supRes.data;
            populateSupplierDropdowns();
        }
    } catch (err) {
        console.error('Failed to load lookup metadata:', err);
    }
}

function populateCategoryDropdowns() {
    const selects = ['productCategorySelect', 'productFilterCategory'];
    selects.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        const currentVal = el.value;
        const isFilter = id === 'productFilterCategory';
        el.innerHTML = isFilter ? '<option value="">All Categories</option>' : '<option value="">Select Category...</option>';
        state.categories.forEach(c => {
            el.innerHTML += `<option value="${c.categoryId}">${escapeHtml(c.categoryName)}</option>`;
        });
        if (currentVal) el.value = currentVal;
    });
}

function populateSupplierDropdowns() {
    const selects = ['productSupplierSelect', 'stockInSupplierSelect'];
    selects.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        const currentVal = el.value;
        el.innerHTML = '<option value="">Select Supplier (Optional)...</option>';
        state.suppliers.forEach(s => {
            el.innerHTML += `<option value="${s.supplierId}">${escapeHtml(s.supplierName)}</option>`;
        });
        if (currentVal) el.value = currentVal;
    });
}

// ==============================================================================
// 3. Dashboard View & Visualizations
// ==============================================================================
async function loadDashboard() {
    try {
        const res = await fetch('/api/dashboard/summary').then(r => r.json());
        if (!res.success) throw new Error(res.message);

        const data = res.data;

        // Metric cards
        document.getElementById('statTotalProducts').textContent = Number(data.totalProducts).toLocaleString();
        document.getElementById('statTotalStockUnits').textContent = Number(data.totalStockUnits).toLocaleString();
        document.getElementById('statLowStockCount').textContent = Number(data.lowStockCount).toLocaleString();
        document.getElementById('statOutOfStockCount').textContent = Number(data.outOfStockCount).toLocaleString();
        document.getElementById('statTotalSuppliers').textContent = Number(data.totalSuppliers).toLocaleString();
        document.getElementById('statInventoryValue').textContent = '$' + Number(data.totalInventoryValue || 0).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});

        // Recent Movements table
        renderRecentMovementsTable(data.recentMovements || []);

        // Low-stock quick list
        renderDashboardLowStockList(data.lowStockProducts || []);

        // Charts
        renderCategoryStockChart(data.categoryStockDistribution || {});
        renderMovementTrendChart(data.movementTrend || {});

    } catch (err) {
        showToast('Error loading dashboard: ' + err.message, 'danger');
    }
}

function renderRecentMovementsTable(movements) {
    const tbody = document.getElementById('dashboardRecentMovementsTableBody');
    if (!tbody) return;

    if (!movements || movements.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-3">No inventory movements recorded yet.</td></tr>`;
        return;
    }

    tbody.innerHTML = movements.map(m => `
        <tr>
            <td class="font-mono text-muted">#${m.movementId}</td>
            <td>
                <strong>${escapeHtml(m.productName)}</strong>
                <div class="small text-muted font-mono">${escapeHtml(m.sku)}</div>
            </td>
            <td>
                <span class="badge-status ${m.movementType === 'STOCK_IN' ? 'badge-movement-in' : 'badge-movement-out'}">
                    <i class="bi ${m.movementType === 'STOCK_IN' ? 'bi-arrow-down-left' : 'bi-arrow-up-right'}"></i>
                    ${m.movementType === 'STOCK_IN' ? 'Stock In' : 'Stock Out'}
                </span>
            </td>
            <td>
                <strong>${m.movementType === 'STOCK_IN' ? '+' : '-'}${m.quantity}</strong>
                <span class="text-muted small">(${m.previousQuantity} &rarr; ${m.newQuantity})</span>
            </td>
            <td class="font-mono small">${escapeHtml(m.referenceNumber || 'N/A')}</td>
            <td class="small text-muted">${formatDate(m.createdAt)}</td>
        </tr>
    `).join('');
}

function renderDashboardLowStockList(products) {
    const container = document.getElementById('dashboardLowStockList');
    if (!container) return;

    if (!products || products.length === 0) {
        container.innerHTML = `
            <div class="text-center py-4 text-success">
                <i class="bi bi-check-circle fs-3 d-block mb-1"></i>
                <span class="small font-medium">All inventory items are currently well-stocked.</span>
            </div>
        `;
        return;
    }

    container.innerHTML = products.slice(0, 5).map(p => `
        <div class="d-flex align-items-center justify-content-between p-2 mb-2 rounded bg-light border border-light">
            <div>
                <strong class="d-block small text-dark">${escapeHtml(p.productName)}</strong>
                <span class="font-mono text-muted" style="font-size: 0.75rem;">${escapeHtml(p.sku)}</span>
            </div>
            <div class="text-end">
                <span class="badge-status ${p.quantity === 0 ? 'badge-out-of-stock' : 'badge-low-stock'}">
                    ${p.quantity === 0 ? 'Out of Stock' : `${p.quantity} / min ${p.minStockLevel}`}
                </span>
                <button class="btn btn-sm btn-link p-0 d-block mt-1 text-decoration-none" style="font-size: 0.72rem;" onclick="quickRestock(${p.productId})">
                    <i class="bi bi-box-arrow-in-down"></i> Restock
                </button>
            </div>
        </div>
    `).join('');
}

function renderCategoryStockChart(distribution) {
    const ctx = document.getElementById('chartCategoryStock');
    if (!ctx) return;

    const labels = Object.keys(distribution);
    const data = Object.values(distribution);

    if (state.charts.categoryStock) {
        state.charts.categoryStock.destroy();
    }

    const colors = ['#2563eb', '#10b981', '#f59e0b', '#06b6d4', '#8b5cf6', '#ec4899', '#64748b'];

    state.charts.categoryStock = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: colors.slice(0, labels.length),
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { boxWidth: 12, font: { size: 11, family: 'Inter' } }
                }
            },
            cutout: '68%'
        }
    });
}

function renderMovementTrendChart(trend) {
    const ctx = document.getElementById('chartMovementTrend');
    if (!ctx) return;

    if (state.charts.movementTrend) {
        state.charts.movementTrend.destroy();
    }

    state.charts.movementTrend = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Stock In (Procurement)', 'Stock Out (Dispatches)'],
            datasets: [{
                label: 'Total Operations',
                data: [trend.STOCK_IN || 0, trend.STOCK_OUT || 0],
                backgroundColor: ['#2563eb', '#f43f5e'],
                borderRadius: 8,
                barThickness: 36
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { precision: 0, font: { size: 11, family: 'Inter' } },
                    grid: { color: '#f1f5f9' }
                },
                x: {
                    grid: { display: false },
                    ticks: { font: { size: 11, family: 'Inter' } }
                }
            }
        }
    });
}

// ==============================================================================
// 4. Products Management View
// ==============================================================================
async function loadProducts() {
    const search = document.getElementById('productSearchInput')?.value || '';
    const catId = document.getElementById('productFilterCategory')?.value || '';
    const status = document.getElementById('productFilterStatus')?.value || 'ALL';

    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (catId) params.append('categoryId', catId);
    if (status) params.append('status', status);

    try {
        const res = await fetch(`/api/products?${params.toString()}`).then(r => r.json());
        if (!res.success) throw new Error(res.message);

        state.products = res.data;
        renderProductsTable(res.data);
    } catch (err) {
        showToast('Error loading products: ' + err.message, 'danger');
    }
}

function renderProductsTable(products) {
    const tbody = document.getElementById('productsTableBody');
    if (!tbody) return;

    if (!products || products.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-muted">No products found matching your filter criteria.</td></tr>`;
        return;
    }

    tbody.innerHTML = products.map(p => {
        let statusBadge = '';
        if (p.quantity === 0) {
            statusBadge = '<span class="badge-status badge-out-of-stock"><i class="bi bi-x-circle"></i> Out of Stock</span>';
        } else if (p.quantity <= p.minStockLevel) {
            statusBadge = '<span class="badge-status badge-low-stock"><i class="bi bi-exclamation-triangle"></i> Low Stock</span>';
        } else {
            statusBadge = '<span class="badge-status badge-normal"><i class="bi bi-check-circle"></i> In Stock</span>';
        }

        const isInactive = p.status === 'INACTIVE';

        return `
            <tr class="${isInactive ? 'opacity-75 bg-light' : ''}">
                <td class="font-mono text-muted">#${p.productId}</td>
                <td>
                    <strong>${escapeHtml(p.productName)}</strong>
                    <div class="small text-muted font-mono">${escapeHtml(p.sku)}</div>
                </td>
                <td><span class="badge bg-secondary-subtle text-secondary border">${escapeHtml(p.categoryName || 'N/A')}</span></td>
                <td><span class="small text-muted">${escapeHtml(p.supplierName || 'Unassigned')}</span></td>
                <td>$${Number(p.purchasePrice).toFixed(2)}</td>
                <td><strong class="text-primary">$${Number(p.sellingPrice).toFixed(2)}</strong></td>
                <td>
                    <span class="fs-6 fw-bold ${p.quantity === 0 ? 'text-danger' : (p.quantity <= p.minStockLevel ? 'text-warning' : 'text-dark')}">
                        ${p.quantity}
                    </span>
                    <span class="text-muted small"> / min ${p.minStockLevel}</span>
                </td>
                <td>
                    ${statusBadge}
                    ${isInactive ? '<span class="badge bg-dark-subtle text-dark ms-1">INACTIVE</span>' : ''}
                </td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-secondary" title="View Details" onclick="viewProductDetails(${p.productId})">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-outline-primary" title="Edit Product" onclick="openEditProductModal(${p.productId})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-outline-success" title="Stock In" onclick="quickStockIn(${p.productId})">
                            <i class="bi bi-box-arrow-in-down"></i>
                        </button>
                        <button class="btn btn-outline-warning" title="Stock Out" onclick="quickStockOut(${p.productId})" ${p.quantity === 0 || isInactive ? 'disabled' : ''}>
                            <i class="bi bi-box-arrow-up-right"></i>
                        </button>
                        <button class="btn ${isInactive ? 'btn-outline-info' : 'btn-outline-danger'}" 
                                title="${isInactive ? 'Reactivate' : 'Deactivate / Delete'}" 
                                onclick="handleProductDeleteOrToggle(${p.productId}, '${p.status}')">
                            <i class="bi ${isInactive ? 'bi-arrow-clockwise' : 'bi-trash'}"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function openAddProductModal() {
    document.getElementById('productFormModalTitle').textContent = 'Add New Product';
    document.getElementById('productForm').reset();
    document.getElementById('productIdInput').value = '';
    document.getElementById('productSkuInput').readOnly = false;
    document.getElementById('productInitialQtyContainer').style.display = 'block';

    const modal = new bootstrap.Modal(document.getElementById('productFormModal'));
    modal.show();
}

function openEditProductModal(id) {
    const product = state.products.find(p => p.productId === id);
    if (!product) return;

    document.getElementById('productFormModalTitle').textContent = 'Edit Product Details';
    document.getElementById('productIdInput').value = product.productId;
    document.getElementById('productSkuInput').value = product.sku;
    document.getElementById('productSkuInput').readOnly = false;
    document.getElementById('productNameInput').value = product.productName;
    document.getElementById('productCategorySelect').value = product.categoryId;
    document.getElementById('productSupplierSelect').value = product.supplierId || '';
    document.getElementById('productPurchasePriceInput').value = product.purchasePrice;
    document.getElementById('productSellingPriceInput').value = product.sellingPrice;
    document.getElementById('productMinStockInput').value = product.minStockLevel;
    document.getElementById('productStatusSelect').value = product.status;

    // Hide initial quantity field on edit
    document.getElementById('productInitialQtyContainer').style.display = 'none';

    const modal = new bootstrap.Modal(document.getElementById('productFormModal'));
    modal.show();
}

async function handleProductFormSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('productIdInput').value;
    const isEdit = Boolean(id);

    const payload = {
        sku: document.getElementById('productSkuInput').value.trim(),
        productName: document.getElementById('productNameInput').value.trim(),
        categoryId: Number(document.getElementById('productCategorySelect').value),
        supplierId: document.getElementById('productSupplierSelect').value ? Number(document.getElementById('productSupplierSelect').value) : null,
        purchasePrice: parseFloat(document.getElementById('productPurchasePriceInput').value),
        sellingPrice: parseFloat(document.getElementById('productSellingPriceInput').value),
        minStockLevel: parseInt(document.getElementById('productMinStockInput').value, 10),
        status: document.getElementById('productStatusSelect').value
    };

    if (!isEdit) {
        payload.initialQuantity = parseInt(document.getElementById('productInitialQtyInput').value || '0', 10);
    }

    try {
        const url = isEdit ? `/api/products/${id}` : '/api/products';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(r => r.json());

        if (!res.success) throw new Error(res.message);

        bootstrap.Modal.getInstance(document.getElementById('productFormModal')).hide();
        showToast(isEdit ? 'Product updated successfully.' : 'Product created and registered into Oracle DB.', 'success');
        loadProducts();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function handleProductDeleteOrToggle(id, currentStatus) {
    if (currentStatus === 'INACTIVE') {
        if (!confirm('Reactivate this product?')) return;
        try {
            const res = await fetch(`/api/products/${id}/activate`, { method: 'PATCH' }).then(r => r.json());
            if (!res.success) throw new Error(res.message);
            showToast('Product reactivated.', 'success');
            loadProducts();
        } catch (err) {
            showToast(err.message, 'danger');
        }
        return;
    }

    if (!confirm('Are you sure you want to delete or deactivate this product? (If transaction history exists, it will be safely deactivated to preserve audit integrity)')) {
        return;
    }

    try {
        const res = await fetch(`/api/products/${id}`, { method: 'DELETE' }).then(r => r.json());
        if (!res.success) throw new Error(res.message);
        showToast(res.message || 'Product processed successfully.', 'info');
        loadProducts();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

function viewProductDetails(id) {
    const product = state.products.find(p => p.productId === id);
    if (!product) return;

    document.getElementById('detailProductName').textContent = product.productName;
    document.getElementById('detailSku').textContent = product.sku;
    document.getElementById('detailCategory').textContent = product.categoryName;
    document.getElementById('detailSupplier').textContent = product.supplierName || 'Unassigned';
    document.getElementById('detailPurchasePrice').textContent = '$' + Number(product.purchasePrice).toFixed(2);
    document.getElementById('detailSellingPrice').textContent = '$' + Number(product.sellingPrice).toFixed(2);
    document.getElementById('detailQuantity').textContent = product.quantity;
    document.getElementById('detailMinStock').textContent = product.minStockLevel;
    document.getElementById('detailStatus').textContent = product.status;
    document.getElementById('detailCreatedAt').textContent = formatDate(product.createdAt);
    document.getElementById('detailUpdatedAt').textContent = formatDate(product.updatedAt);

    const modal = new bootstrap.Modal(document.getElementById('productDetailModal'));
    modal.show();
}

// ==============================================================================
// 5. Stock In (Procurement / Receiving)
// ==============================================================================
function prepareStockInForm() {
    populateProductSelect('stockInProductSelect');
    populateSupplierDropdowns();
    document.getElementById('stockInForm').reset();
    document.getElementById('stockInDateInput').value = new Date().toISOString().slice(0, 10);
    document.getElementById('stockInCurrentStockDisplay').textContent = '--';
}

function populateProductSelect(elementId) {
    const el = document.getElementById(elementId);
    if (!el) return;
    el.innerHTML = '<option value="">Select Target Product...</option>';
    state.products.forEach(p => {
        el.innerHTML += `<option value="${p.productId}" data-qty="${p.quantity}" data-price="${p.purchasePrice}">${escapeHtml(p.productName)} (SKU: ${escapeHtml(p.sku)} - In Stock: ${p.quantity})</option>`;
    });

    el.addEventListener('change', () => {
        const opt = el.selectedOptions[0];
        if (opt && opt.value) {
            const qty = opt.getAttribute('data-qty');
            const price = opt.getAttribute('data-price');
            if (elementId === 'stockInProductSelect') {
                document.getElementById('stockInCurrentStockDisplay').textContent = `${qty} units currently available`;
                if (price) document.getElementById('stockInPriceInput').value = price;
            } else if (elementId === 'stockOutProductSelect') {
                document.getElementById('stockOutCurrentStockDisplay').textContent = `${qty} units currently available`;
            }
        }
    });
}

function quickStockIn(productId) {
    navigateTo('stock-in');
    setTimeout(() => {
        const sel = document.getElementById('stockInProductSelect');
        if (sel) {
            sel.value = productId;
            sel.dispatchEvent(new Event('change'));
        }
    }, 150);
}

function quickRestock(productId) {
    quickStockIn(productId);
}

async function handleStockInSubmit(e) {
    e.preventDefault();

    const productId = Number(document.getElementById('stockInProductSelect').value);
    const quantity = parseInt(document.getElementById('stockInQuantityInput').value, 10);
    const supplierId = document.getElementById('stockInSupplierSelect').value ? Number(document.getElementById('stockInSupplierSelect').value) : null;
    const unitPrice = document.getElementById('stockInPriceInput').value ? parseFloat(document.getElementById('stockInPriceInput').value) : null;
    const referenceNumber = document.getElementById('stockInRefInput').value.trim();
    const remarks = document.getElementById('stockInRemarksInput').value.trim();

    if (!productId || isNaN(quantity) || quantity <= 0) {
        showToast('Please select a valid product and enter a positive quantity.', 'warning');
        return;
    }

    if (!confirm(`Confirm receiving ${quantity} units into inventory? This will atomically update stock balance via PL/SQL INVENTORY_PKG.STOCK_IN.`)) {
        return;
    }

    try {
        const res = await fetch('/api/inventory/stock-in', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productId,
                supplierId,
                quantity,
                unitPrice,
                referenceNumber,
                remarks
            })
        }).then(r => r.json());

        if (!res.success) throw new Error(res.message);

        showToast(`Stock-In Successful! New Stock Balance: ${res.data.newQuantity} units.`, 'success');
        document.getElementById('stockInForm').reset();
        loadLookupData();
        navigateTo('movements');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ==============================================================================
// 6. Stock Out (Sales / Issue Dispatch)
// ==============================================================================
function prepareStockOutForm() {
    populateProductSelect('stockOutProductSelect');
    document.getElementById('stockOutForm').reset();
    document.getElementById('stockOutDateInput').value = new Date().toISOString().slice(0, 10);
    document.getElementById('stockOutCurrentStockDisplay').textContent = '--';
}

function quickStockOut(productId) {
    navigateTo('stock-out');
    setTimeout(() => {
        const sel = document.getElementById('stockOutProductSelect');
        if (sel) {
            sel.value = productId;
            sel.dispatchEvent(new Event('change'));
        }
    }, 150);
}

async function handleStockOutSubmit(e) {
    e.preventDefault();

    const productId = Number(document.getElementById('stockOutProductSelect').value);
    const quantity = parseInt(document.getElementById('stockOutQuantityInput').value, 10);
    const referenceNumber = document.getElementById('stockOutRefInput').value.trim();
    const remarks = document.getElementById('stockOutRemarksInput').value.trim();

    if (!productId || isNaN(quantity) || quantity <= 0) {
        showToast('Please select a valid product and enter a positive issue quantity.', 'warning');
        return;
    }

    if (!confirm(`Confirm issuing / selling ${quantity} units from inventory? This operation locks the database row and prevents negative balances.`)) {
        return;
    }

    try {
        const res = await fetch('/api/inventory/stock-out', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productId,
                quantity,
                referenceNumber,
                remarks
            })
        }).then(r => r.json());

        if (!res.success) throw new Error(res.message);

        showToast(`Stock-Out Successful! Updated Balance: ${res.data.newQuantity} units.`, 'success');
        document.getElementById('stockOutForm').reset();
        loadLookupData();
        navigateTo('movements');
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ==============================================================================
// 7. Transaction History View
// ==============================================================================
async function loadMovements() {
    const type = document.getElementById('movementsFilterType')?.value || 'ALL';
    const search = document.getElementById('movementsSearchInput')?.value || '';

    const params = new URLSearchParams();
    if (type && type !== 'ALL') params.append('movementType', type);
    if (search) params.append('search', search);
    params.append('limit', '100');

    try {
        const res = await fetch(`/api/inventory/movements?${params.toString()}`).then(r => r.json());
        if (!res.success) throw new Error(res.message);

        renderMovementsTable(res.data);
    } catch (err) {
        showToast('Error loading movements: ' + err.message, 'danger');
    }
}

function renderMovementsTable(movements) {
    const tbody = document.getElementById('movementsTableBody');
    if (!tbody) return;

    if (!movements || movements.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No inventory transaction records match your filters.</td></tr>`;
        return;
    }

    tbody.innerHTML = movements.map(m => `
        <tr>
            <td class="font-mono text-muted">#${m.movementId}</td>
            <td>
                <strong>${escapeHtml(m.productName)}</strong>
                <div class="small text-muted font-mono">${escapeHtml(m.sku)}</div>
            </td>
            <td>
                <span class="badge-status ${m.movementType === 'STOCK_IN' ? 'badge-movement-in' : 'badge-movement-out'}">
                    <i class="bi ${m.movementType === 'STOCK_IN' ? 'bi-box-arrow-in-down' : 'bi-box-arrow-up-right'}"></i>
                    ${m.movementType === 'STOCK_IN' ? 'STOCK IN' : 'STOCK OUT'}
                </span>
            </td>
            <td>
                <strong class="${m.movementType === 'STOCK_IN' ? 'text-primary' : 'text-danger'}">
                    ${m.movementType === 'STOCK_IN' ? '+' : '-'}${m.quantity}
                </strong>
            </td>
            <td>
                <span class="small font-mono text-muted">${m.previousQuantity}</span>
                <i class="bi bi-arrow-right text-muted small mx-1"></i>
                <span class="small font-mono fw-bold text-dark">${m.newQuantity}</span>
            </td>
            <td class="font-mono small">${escapeHtml(m.referenceNumber || 'N/A')}</td>
            <td class="small text-secondary">${escapeHtml(m.remarks || '—')}</td>
            <td class="small text-muted font-mono">${formatDate(m.createdAt)}</td>
        </tr>
    `).join('');
}

// ==============================================================================
// 8. Low-Stock Alerts View
// ==============================================================================
async function loadLowStockAlerts() {
    try {
        const res = await fetch('/api/inventory/low-stock').then(r => r.json());
        if (!res.success) throw new Error(res.message);

        renderLowStockTable(res.data);
    } catch (err) {
        showToast('Error loading low-stock alerts: ' + err.message, 'danger');
    }
}

function renderLowStockTable(alerts) {
    const tbody = document.getElementById('lowStockTableBody');
    if (!tbody) return;

    if (!alerts || alerts.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-success">
                    <i class="bi bi-shield-check fs-2 d-block mb-2"></i>
                    <h5>All Products Sufficiently Stocked</h5>
                    <p class="small text-muted mb-0">No products are currently at or below minimum threshold.</p>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = alerts.map(p => `
        <tr>
            <td class="font-mono text-muted">#${p.productId}</td>
            <td>
                <strong>${escapeHtml(p.productName)}</strong>
                <div class="small text-muted font-mono">${escapeHtml(p.sku)}</div>
            </td>
            <td><span class="badge bg-secondary-subtle text-secondary border">${escapeHtml(p.categoryName || 'N/A')}</span></td>
            <td>${escapeHtml(p.supplierName || 'Unassigned')}</td>
            <td>
                <span class="badge-status ${p.quantity === 0 ? 'badge-out-of-stock' : 'badge-low-stock'}">
                    <i class="bi ${p.quantity === 0 ? 'bi-slash-circle' : 'bi-exclamation-triangle-fill'}"></i>
                    ${p.quantity === 0 ? 'OUT OF STOCK' : 'LOW STOCK'}
                </span>
            </td>
            <td>
                <strong class="${p.quantity === 0 ? 'text-danger' : 'text-warning'} fs-6">${p.quantity}</strong>
                <span class="text-muted small"> / threshold ${p.minStockLevel}</span>
            </td>
            <td class="text-end">
                <button class="btn btn-sm btn-accent" onclick="quickRestock(${p.productId})">
                    <i class="bi bi-box-arrow-in-down me-1"></i> Receive Stock
                </button>
            </td>
        </tr>
    `).join('');
}

// ==============================================================================
// 9. Suppliers Management View
// ==============================================================================
async function loadSuppliers() {
    try {
        const res = await fetch('/api/suppliers').then(r => r.json());
        if (!res.success) throw new Error(res.message);

        state.suppliers = res.data;
        renderSuppliersTable(res.data);
    } catch (err) {
        showToast('Error loading suppliers: ' + err.message, 'danger');
    }
}

function renderSuppliersTable(suppliers) {
    const tbody = document.getElementById('suppliersTableBody');
    if (!tbody) return;

    if (!suppliers || suppliers.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No suppliers registered.</td></tr>`;
        return;
    }

    tbody.innerHTML = suppliers.map(s => `
        <tr>
            <td class="font-mono text-muted">#${s.supplierId}</td>
            <td><strong>${escapeHtml(s.supplierName)}</strong></td>
            <td>${escapeHtml(s.phone || '—')}</td>
            <td><a href="mailto:${encodeURIComponent(s.email || '')}" class="text-decoration-none">${escapeHtml(s.email || '—')}</a></td>
            <td class="small text-secondary">${escapeHtml(s.address || '—')}</td>
            <td>
                <span class="badge bg-primary-subtle text-primary border">${s.associatedProductsCount} products</span>
            </td>
            <td class="text-end">
                <div class="btn-group btn-group-sm">
                    <button class="btn btn-outline-primary" onclick="openEditSupplierModal(${s.supplierId})" title="Edit">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-outline-danger" onclick="deleteSupplier(${s.supplierId})" title="Delete" ${s.associatedProductsCount > 0 ? 'disabled' : ''}>
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </td>
        </tr>
    `).join('');
}

function openAddSupplierModal() {
    document.getElementById('supplierModalTitle').textContent = 'Add New Supplier';
    document.getElementById('supplierForm').reset();
    document.getElementById('supplierIdInput').value = '';
    new bootstrap.Modal(document.getElementById('supplierModal')).show();
}

function openEditSupplierModal(id) {
    const s = state.suppliers.find(sup => sup.supplierId === id);
    if (!s) return;

    document.getElementById('supplierModalTitle').textContent = 'Edit Supplier';
    document.getElementById('supplierIdInput').value = s.supplierId;
    document.getElementById('supplierNameInput').value = s.supplierName;
    document.getElementById('supplierPhoneInput').value = s.phone || '';
    document.getElementById('supplierEmailInput').value = s.email || '';
    document.getElementById('supplierAddressInput').value = s.address || '';

    new bootstrap.Modal(document.getElementById('supplierModal')).show();
}

async function handleSupplierFormSubmit(e) {
    e.preventDefault();
    const id = document.getElementById('supplierIdInput').value;
    const isEdit = Boolean(id);

    const payload = {
        supplierName: document.getElementById('supplierNameInput').value.trim(),
        phone: document.getElementById('supplierPhoneInput').value.trim(),
        email: document.getElementById('supplierEmailInput').value.trim(),
        address: document.getElementById('supplierAddressInput').value.trim()
    };

    try {
        const url = isEdit ? `/api/suppliers/${id}` : '/api/suppliers';
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        }).then(r => r.json());

        if (!res.success) throw new Error(res.message);

        bootstrap.Modal.getInstance(document.getElementById('supplierModal')).hide();
        showToast(isEdit ? 'Supplier updated.' : 'Supplier added successfully.', 'success');
        loadSuppliers();
        loadLookupData();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

async function deleteSupplier(id) {
    if (!confirm('Are you sure you want to delete this supplier?')) return;
    try {
        const res = await fetch(`/api/suppliers/${id}`, { method: 'DELETE' }).then(r => r.json());
        if (!res.success) throw new Error(res.message);
        showToast('Supplier deleted.', 'info');
        loadSuppliers();
        loadLookupData();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ==============================================================================
// 10. Category Creation Modal
// ==============================================================================
function openAddCategoryModal() {
    document.getElementById('categoryForm').reset();
    new bootstrap.Modal(document.getElementById('categoryModal')).show();
}

async function handleCategoryFormSubmit(e) {
    e.preventDefault();
    const name = document.getElementById('categoryNameInput').value.trim();
    const desc = document.getElementById('categoryDescInput').value.trim();

    try {
        const res = await fetch('/api/categories', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ categoryName: name, description: desc })
        }).then(r => r.json());

        if (!res.success) throw new Error(res.message);

        bootstrap.Modal.getInstance(document.getElementById('categoryModal')).hide();
        showToast('Category created.', 'success');
        loadLookupData();
    } catch (err) {
        showToast(err.message, 'danger');
    }
}

// ==============================================================================
// 11. Helper Utilities
// ==============================================================================
function setupFormHandlers() {
    document.getElementById('productForm')?.addEventListener('submit', handleProductFormSubmit);
    document.getElementById('stockInForm')?.addEventListener('submit', handleStockInSubmit);
    document.getElementById('stockOutForm')?.addEventListener('submit', handleStockOutSubmit);
    document.getElementById('supplierForm')?.addEventListener('submit', handleSupplierFormSubmit);
    document.getElementById('categoryForm')?.addEventListener('submit', handleCategoryFormSubmit);

    // Filters
    document.getElementById('productSearchInput')?.addEventListener('input', debounce(loadProducts, 300));
    document.getElementById('productFilterCategory')?.addEventListener('change', loadProducts);
    document.getElementById('productFilterStatus')?.addEventListener('change', loadProducts);

    document.getElementById('movementsSearchInput')?.addEventListener('input', debounce(loadMovements, 300));
    document.getElementById('movementsFilterType')?.addEventListener('change', loadMovements);
}

function showToast(message, type = 'info') {
    const toastEl = document.getElementById('liveToast');
    const toastBody = document.getElementById('toastBodyText');
    const toastHeader = document.getElementById('toastHeaderTitle');
    const toastIcon = document.getElementById('toastIcon');

    if (!toastEl || !toastBody) return;

    toastBody.textContent = message;

    const config = {
        'success': { title: 'Success', icon: 'bi-check-circle-fill text-success' },
        'danger': { title: 'Error', icon: 'bi-exclamation-octagon-fill text-danger' },
        'warning': { title: 'Warning', icon: 'bi-exclamation-triangle-fill text-warning' },
        'info': { title: 'Notification', icon: 'bi-info-circle-fill text-primary' }
    }[type] || { title: 'Notice', icon: 'bi-bell text-secondary' };

    toastHeader.textContent = config.title;
    toastIcon.className = `bi ${config.icon} me-2`;

    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();
}

function formatDate(dtStr) {
    if (!dtStr) return 'N/A';
    try {
        const d = new Date(dtStr);
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return dtStr;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function debounce(fn, delay) {
    let timer = null;
    return function (...args) {
        clearTimeout(timer);
        timer = setTimeout(() => fn.apply(this, args), delay);
    };
}
