const API_URL = "http://127.0.0.1:8080/api";

function showToast(message) {
    const toast = document.getElementById('sys-msg');
    toast.innerText = message;
    toast.style.display = 'block';
    setTimeout(() => { toast.style.display = 'none'; }, 4000);
}

// ==========================================
// --- SISTEMA DE DIÁLOGOS PERSONALIZADO ---
// ==========================================
const CustomDialog = {
    show: function(options) {
        return new Promise((resolve) => {
            const modal = document.getElementById('custom-dialog');
            document.getElementById('dialog-title').innerText = options.title || 'Notificación';
            document.getElementById('dialog-message').innerText = options.message || '';
            const input = document.getElementById('dialog-input');
            const btnCancel = document.getElementById('dialog-cancel');
            const btnConfirm = document.getElementById('dialog-confirm');

            if (options.type === 'prompt') {
                input.style.display = 'block';
                input.value = '';
                input.focus();
            } else {
                input.style.display = 'none';
            }

            if (options.type === 'alert') {
                btnCancel.style.display = 'none';
            } else {
                btnCancel.style.display = 'inline-block';
            }

            modal.style.display = 'flex';

            btnConfirm.onclick = () => {
                modal.style.display = 'none';
                resolve(options.type === 'prompt' ? input.value : true);
            };

            btnCancel.onclick = () => {
                modal.style.display = 'none';
                resolve(options.type === 'prompt' ? null : false);
            };
        });
    },
    alert: (message, title) => CustomDialog.show({ type: 'alert', message, title }),
    confirm: (message, title) => CustomDialog.show({ type: 'confirm', message, title }),
    prompt: (message, title) => CustomDialog.show({ type: 'prompt', message, title })
};

// ==========================================
// --- GESTIÓN DE SESIÓN Y VISTAS ---
// ==========================================
function checkLoginState() {
    const token = localStorage.getItem('jwtToken');
    const user = localStorage.getItem('username');
    if (token && user) {
        document.getElementById('auth-view').style.display = 'none';
        document.getElementById('dashboard-view').style.display = 'flex';
        document.getElementById('display-user').innerText = user;
        
        const secretBtn = document.getElementById('btn-secret-admin');
        if (secretBtn) secretBtn.style.display = 'inline';
        
        updateBalanceUI();
        checkIfAdmin(); 
    } else {
        document.getElementById('auth-view').style.display = 'flex';
        document.getElementById('dashboard-view').style.display = 'none';
    }
}
window.onload = checkLoginState;

document.getElementById('btn-logout').addEventListener('click', () => {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('username');
    
    const adminPanel = document.getElementById('admin-panel');
    if(adminPanel) adminPanel.style.display = 'none';
    
    const secretBtn = document.getElementById('btn-secret-admin');
    if(secretBtn) secretBtn.style.display = 'inline';
    
    checkLoginState();
});

// ==========================================
// --- AUTENTICACIÓN (LOGIN/REGISTRO) ---
// ==========================================
document.getElementById('login-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = document.getElementById('login-username').value;
    const pass = document.getElementById('login-password').value;
    
    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });
        
        let rawData = await response.text();

        if (rawData.includes("Error") || rawData.includes("inválidas") || rawData.includes("inválido")) {
            showToast(rawData);
            return; 
        }
        
        if (response.ok) {
            let tokenToSave = rawData;
            
            try {
                const parsed = JSON.parse(rawData);
                if (parsed.token) tokenToSave = parsed.token;
            } catch (err) {} 
            tokenToSave = tokenToSave.replace(/^"|"$/g, '').trim();

            localStorage.setItem('jwtToken', tokenToSave);
            localStorage.setItem('username', user); 
            checkLoginState();
            showToast("¡Bienvenido a Power Casino!");
        } else {
            showToast("Error: Credenciales incorrectas.");
        }
    } catch(err) {
        showToast("Error conectando con el servidor.");
    }
});

document.getElementById('register-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = document.getElementById('reg-username').value;
    const pass = document.getElementById('reg-password').value;
    const response = await fetch(`${API_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: user, password: pass })
    });
    
    if(response.ok) {
        let text = await response.text();
        if(text.includes("Error")) {
            showToast(text);
        } else {
            showToast("¡Cuenta creada! Ya puedes iniciar sesión.");
            document.getElementById('reg-username').value = "";
            document.getElementById('reg-password').value = "";
        }
    } else {
        showToast("Error al registrar. El usuario podría ya existir.");
    }
});

// ==========================================
// --- BILLETERA Y SALDO ---
// ==========================================
async function updateBalanceUI() {
    const token = localStorage.getItem('jwtToken');
    if(!token) return;
    try {
        const res = await fetch(`${API_URL}/wallet/balance`, { headers: { 'Authorization': `Bearer ${token}` } });
        if(res.ok) {
            const text = await res.text();
            const match = text.match(/\$([0-9.]+)/);
            document.getElementById('display-balance').innerText = match ? match[1] : "0.00";
        } else if (res.status === 403) {
            document.getElementById('btn-logout').click();
            await CustomDialog.alert("Tu sesión expiró o tu token es inválido. Inicia sesión nuevamente.", "Sesión Finalizada");
        } else {
            document.getElementById('display-balance').innerText = "0.00";
        }
    } catch(e) { document.getElementById('display-balance').innerText = "0.00"; }
}

document.getElementById('btn-deposit').addEventListener('click', async () => {
    const card = document.getElementById('dep-card').value;
    const amount = parseFloat(document.getElementById('dep-amount').value);
    const user = localStorage.getItem('username');
    const token = localStorage.getItem('jwtToken');

    if (!amount || isNaN(amount)) return showToast("Ingresa un monto válido para depositar.");
    
    if (amount <= 0) {
        return showToast("Error: El depósito debe ser mayor a $0.");
    }

    const response = await fetch(`${API_URL}/wallet/vip-deposit/${user}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ cardNumber: card, amount: amount })
    });
    showToast(await response.text());
    
    if (response.ok) {
        document.getElementById('dep-card').value = '';
        document.getElementById('dep-amount').value = '';
        updateBalanceUI();
    }
});

document.getElementById('btn-withdraw').addEventListener('click', async () => {
    const card = document.getElementById('withdraw-card').value;
    const amount = parseFloat(document.getElementById('withdraw-amount').value);
    const user = localStorage.getItem('username');
    const token = localStorage.getItem('jwtToken');

    if (!amount || isNaN(amount)) return showToast("Ingresa un monto válido para retirar.");
    
    if (amount <= 0) {
        return showToast("Error: El retiro debe ser mayor a $0.");
    }

    const response = await fetch(`${API_URL}/wallet/withdraw/${user}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ cardNumber: card, amount: amount })
    });
    
    showToast(await response.text());
    
    if (response.ok) {
        document.getElementById('withdraw-card').value = ''; 
        document.getElementById('withdraw-amount').value = ''; 
        updateBalanceUI();
    }
});

// ==========================================
// --- PERFIL DE USUARIO ---
// ==========================================
document.getElementById('btn-delete-self').addEventListener('click', async () => {
    const confirmed = await CustomDialog.confirm("¿Estás seguro de eliminar tu propia cuenta? Esta acción no se puede deshacer.", "Borrar Cuenta");
    if(!confirmed) return;
    
    const token = localStorage.getItem('jwtToken');
    const response = await fetch(`${API_URL}/user/delete-account`, { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
    showToast(await response.text());
    if(response.ok) { document.getElementById('btn-logout').click(); }
});

// ==========================================
// --- ZONA ADMINISTRADOR Y ROLES ---
// ==========================================
async function checkIfAdmin() {
    const token = localStorage.getItem('jwtToken');
    if(!token) return;
    try {
        const res = await fetch(`${API_URL}/admin/players`, { headers: { 'Authorization': `Bearer ${token}` } });
        if (res.ok) {
            document.getElementById('admin-panel').style.display = 'block';
            document.getElementById('btn-secret-admin').style.display = 'none';
        }
    } catch(e) {}
}

const btnSecretAdmin = document.getElementById('btn-secret-admin');
if(btnSecretAdmin) {
    btnSecretAdmin.addEventListener('click', async () => {
        const secretInput = await CustomDialog.prompt("Ingrese la clave secreta de Administrador:", "Activar Modo Admin");
        if(!secretInput) return;

        const token = localStorage.getItem('jwtToken');
        const response = await fetch(`${API_URL}/wallet/make-me-admin`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
            body: JSON.stringify({ secretKey: secretInput })
        });
        
        if(response.ok) {
            await CustomDialog.alert("¡Permisos de Administrador concedidos! Vuelve a iniciar sesión para aplicar los cambios.", "Acceso Concedido");
            document.getElementById('btn-logout').click(); 
        } else {
            showToast("Clave incorrecta.");
        }
    });
}

document.getElementById('btn-get-players').addEventListener('click', async () => {
    const token = localStorage.getItem('jwtToken');
    const response = await fetch(`${API_URL}/admin/players`, { headers: { 'Authorization': `Bearer ${token}` } });
    
    if (response.ok) {
        const data = await response.json();
        const tbody = document.getElementById('admin-users-table');
        tbody.innerHTML = ''; 
        
        data.forEach(player => {
            const role = player.role || "USER"; 
            
            const tr = document.createElement('tr');
            tr.style.borderBottom = '1px solid #333';
            
            tr.innerHTML = `
                <td style="padding: 12px;"><strong>${player.username}</strong></td>
                <td style="padding: 12px;">${role === 'ROLE_ADMIN' || role === 'ADMIN' ? '<span style="color:var(--gold)">Administrador</span>' : 'Jugador Normal'}</td>
                <td style="padding: 12px;">
                    <button onclick="auditarUsuarioAdmin('${player.username}')" class="btn-outline" style="border-color: #0dcaf0; color: #0dcaf0; padding: 5px 15px; font-size: 12px; width: auto; cursor: pointer; background: transparent; border-radius: 5px; border-width: 1px; border-style: solid;">Auditar Jugador</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        
        document.getElementById('admin-modal').style.display = 'flex';
    } else {
        showToast("Error 403: No tienes el ROL de Administrador.");
    }
});

window.auditarUsuarioAdmin = async function(targetUser) {
    const token = localStorage.getItem('jwtToken');

    try {
        const res = await fetch(`${API_URL}/admin/player/${targetUser}`, { 
            headers: { 'Authorization': `Bearer ${token}` } 
        });
        
        if (res.ok) {
            const data = await res.json();
            
            document.getElementById('audit-username').innerText = data.username;
            document.getElementById('audit-role').innerText = data.role || "USER";
            document.getElementById('audit-balance').innerText = data.balance ? data.balance.toFixed(2) : "0.00";

            const tbody = document.getElementById('audit-transactions-table');
            tbody.innerHTML = '';
            
            if (data.transactions && data.transactions.length > 0) {
                data.transactions.slice().reverse().forEach(t => {
                    const tr = document.createElement('tr');
                    tr.style.borderBottom = '1px solid #333';
                    
                    let dateStr = "N/A";
                    if (t.timestamp) {
                        dateStr = Array.isArray(t.timestamp) 
                            ? new Date(t.timestamp[0], t.timestamp[1]-1, t.timestamp[2], t.timestamp[3], t.timestamp[4]).toLocaleString()
                            : new Date(t.timestamp).toLocaleString();
                    }

                    const isNegative = t.amount < 0;
                    const amountColor = isNegative ? "var(--danger)" : "var(--casino-green)";

                    tr.innerHTML = `
                        <td style="padding: 10px; color: #aaa;">${dateStr}</td>
                        <td style="padding: 10px;">${t.type}</td>
                        <td style="padding: 10px; text-align: right; color: ${amountColor}; font-weight: bold;">
                            ${isNegative ? '' : '+'}$${Math.abs(t.amount).toFixed(2)}
                        </td>
                        <td style="padding: 10px; text-align: right; color: #fff;">$${t.balanceAfter.toFixed(2)}</td>
                    `;
                    tbody.appendChild(tr);
                });
            } else {
                tbody.innerHTML = `<tr><td colspan="4" style="padding: 15px; text-align: center; color: #666;">El usuario no tiene transacciones registradas.</td></tr>`;
            }

            document.getElementById('audit-modal').style.display = 'flex';
        } else {
            const errorMsg = await res.text();
            showToast(errorMsg);
        }
    } catch(e) {
        showToast("Error conectando con el servidor para la auditoría.");
    }
};

// ==========================================
// --- LÓGICA DE MESA DE BLACKJACK ---
// ==========================================
function getCardHTML(cardData) {
    let symbol = ''; let colorClass = 'black';
    if(cardData.suit === 'Corazones') { symbol = '♥'; colorClass = 'red'; }
    else if(cardData.suit === 'Diamantes') { symbol = '♦'; colorClass = 'red'; }
    else if(cardData.suit === 'Espadas') { symbol = '♠'; colorClass = 'black'; }
    else if(cardData.suit === 'Tréboles') { symbol = '♣'; colorClass = 'black'; }
    
    return `
        <div class="playing-card ${colorClass}">
            <div style="align-self: flex-start;">${cardData.rank} ${symbol}</div>
            <div class="card-center">${symbol}</div>
            <div style="align-self: flex-end; transform: rotate(180deg);">${cardData.rank} ${symbol}</div>
        </div>
    `;
}

function updateGameUI(data) {
    if (typeof data === "string") return showToast(data);

    const renderHand = (hand) => hand ? hand.map(c => getCardHTML(c)).join('') : "";
    document.getElementById('player-cards').innerHTML = renderHand(data.playerHand);
    document.getElementById('dealer-cards').innerHTML = renderHand(data.dealerHand);
    
    document.getElementById('player-score').innerText = data.playerScore || 0;
    document.getElementById('dealer-score').innerText = data.dealerScore || "?";

    let message = data.sideBetsMessage || "";
    if(data.status === "WON") message += " ¡GANASTE LA MANO!";
    if(data.status === "LOST") message += " CRUPIER GANA.";
    if(data.status === "TIE") message += " EMPATE.";
    
    document.getElementById('game-message').innerText = message || "Tu turno...";
    document.getElementById('game-message').style.color = data.status === "PLAYING" ? "#fff" : "var(--gold)";

    updateBalanceUI();

    const isPlaying = data.status === "PLAYING";
    document.getElementById('btn-hit').disabled = !isPlaying;
    document.getElementById('btn-stand').disabled = !isPlaying;
    document.getElementById('btn-double').disabled = !(isPlaying && data.playerHand && data.playerHand.length === 2);
    
    const dealerShowsAce = data.dealerHand && data.dealerHand.length > 0 && data.dealerHand[0].rank === 'A';
    document.getElementById('btn-insurance').disabled = !(isPlaying && data.playerHand.length === 2 && dealerShowsAce);
    
    document.getElementById('btn-start').disabled = isPlaying;
}

document.getElementById('btn-start').addEventListener('click', async () => {
    const amount = parseFloat(document.getElementById('bet-amount').value);
    const perfectPairsBet = parseFloat(document.getElementById('pair-bet-amount').value) || 0;
    const token = localStorage.getItem('jwtToken');

    if (!amount || amount <= 0) return showToast("Ingresa una apuesta principal.");

    document.getElementById('game-message').innerText = "Repartiendo...";
    document.getElementById('player-cards').innerHTML = "";
    document.getElementById('dealer-cards').innerHTML = "";

    const res = await fetch(`${API_URL}/blackjack/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ amount: amount, perfectPairsBet: perfectPairsBet })
    });
    
    const text = await res.text();
    try { updateGameUI(JSON.parse(text)); } catch (e) { showToast(text); }
});

document.getElementById('btn-hit').addEventListener('click', async () => {
    const token = localStorage.getItem('jwtToken');
    const res = await fetch(`${API_URL}/blackjack/hit`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }});
    const text = await res.text();
    try { updateGameUI(JSON.parse(text)); } catch(e) { showToast(text); }
});

document.getElementById('btn-stand').addEventListener('click', async () => {
    const token = localStorage.getItem('jwtToken');
    const res = await fetch(`${API_URL}/blackjack/stand`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }});
    const text = await res.text();
    try { updateGameUI(JSON.parse(text)); } catch(e) { showToast(text); }
});

document.getElementById('btn-double').addEventListener('click', async () => {
    const token = localStorage.getItem('jwtToken');
    const res = await fetch(`${API_URL}/blackjack/double-down`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }});
    const text = await res.text();
    try { updateGameUI(JSON.parse(text)); } catch (e) { showToast(text); }
});

document.getElementById('btn-insurance').addEventListener('click', async () => {
    const token = localStorage.getItem('jwtToken');
    const res = await fetch(`${API_URL}/blackjack/buy-insurance`, { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }});
    const text = await res.text();
    try { updateGameUI(JSON.parse(text)); } catch (e) { showToast(text); }
});