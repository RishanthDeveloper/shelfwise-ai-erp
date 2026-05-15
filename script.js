// Mock Retail Inventory Data
let inventoryBatches = [
    { id: "BCH-8821", category: "Dairy / Perishables", expiry: 2, risk: "Pending", route: "Awaiting Analysis", status: "Pending" },
    { id: "BCH-8822", category: "Canned Goods", expiry: 180, risk: "Pending", route: "Awaiting Analysis", status: "Pending" },
    { id: "BCH-8823", category: "Fresh Produce", expiry: 4, risk: "Pending", route: "Awaiting Analysis", status: "Pending" },
    { id: "BCH-8824", category: "Bakery Items", expiry: 1, risk: "Pending", route: "Awaiting Analysis", status: "Pending" },
    { id: "BCH-8825", category: "Frozen Meats", expiry: 45, risk: "Pending", route: "Awaiting Analysis", status: "Pending" }
];

let aiHasRun = false;

// Render the initial table
function renderTable() {
    const tbody = document.getElementById('inventoryBody');
    tbody.innerHTML = '';
    
    let highRiskCount = 0;

    inventoryBatches.forEach(batch => {
        let riskBadge = `<span class="badge" style="background:#334155; color:#cbd5e1;">${batch.risk}</span>`;
        
        if (batch.risk === "High Risk") {
            riskBadge = `<span class="badge badge-high">${batch.risk}</span>`;
            highRiskCount++;
        } else if (batch.risk === "Medium Risk") {
            riskBadge = `<span class="badge badge-med">${batch.risk}</span>`;
        } else if (batch.risk === "Low Risk") {
            riskBadge = `<span class="badge badge-low">${batch.risk}</span>`;
        }

        let routeDisplay = batch.route === "Awaiting Analysis" 
            ? `<span style="color:#64748b;">${batch.route}</span>` 
            : `<span class="route-text"><i class="fa-solid fa-code-branch"></i> ${batch.route}</span>`;

        let statusIcon = batch.status === "Pending" 
            ? `<i class="fa-solid fa-hourglass-half" style="color:#94a3b8;"></i> Pending`
            : `<i class="fa-solid fa-check" style="color:#10b981;"></i> Executed`;

        tbody.innerHTML += `
            <tr>
                <td><strong>${batch.id}</strong></td>
                <td>${batch.category}</td>
                <td>${batch.expiry} Days</td>
                <td>${riskBadge}</td>
                <td>${routeDisplay}</td>
                <td>${statusIcon}</td>
            </tr>
        `;
    });

    // Update KPIs
    document.getElementById('kpi-total').innerText = inventoryBatches.length;
    document.getElementById('kpi-high-risk').innerText = aiHasRun ? highRiskCount : 0;
    document.getElementById('kpi-routed').innerText = aiHasRun ? inventoryBatches.length : 0;
}

// Simulate XGBoost Classification and DQN Routing
function runAIAnalysis() {
    const btn = document.querySelector('.btn-ai');
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Running Models...';
    
    // Simulate API delay for dramatic effect
    setTimeout(() => {
        inventoryBatches = inventoryBatches.map(batch => {
            // Simulated XGBoost Logic based on expiry
            if (batch.expiry <= 3) {
                batch.risk = "High Risk";
                // Simulated DQN Routing Action
                batch.route = "API_CALL: Auto-Discount 40% & Move to Front";
            } else if (batch.expiry <= 10) {
                batch.risk = "Medium Risk";
                batch.route = "API_CALL: Flag for Review & 10% Promo";
            } else {
                batch.risk = "Low Risk";
                batch.route = "LOG: Maintain Standard Positioning";
            }
            batch.status = "Executed";
            return batch;
        });

        aiHasRun = true;
        renderTable();
        btn.innerHTML = '<i class="fa-solid fa-check"></i> Analysis Complete';
        btn.style.background = "var(--success)";
    }, 1500); // 1.5 second simulated loading time
}

// Initialize
window.onload = renderTable;
