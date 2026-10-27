// Disaster Relief System - Frontend Application Logic
let authToken = localStorage.getItem('drs_token') || null;
let currentUser = null;
let allRegions = [];
let allRoads = [];
let allDepots = [];
let currentPreviewRequestId = null;

// ---------------------------------------------------------------------------------------------
// THEME SWITCHING ENGINE (4 CURATED THEMES)
// ---------------------------------------------------------------------------------------------
const AVAILABLE_THEMES = ['tactical-midnight', 'emerald-sentinel', 'cyberpunk-neon', 'arctic-light'];
let currentTheme = localStorage.getItem('drs_theme') || 'tactical-midnight';

function setAppTheme(themeName) {
  if (!AVAILABLE_THEMES.includes(themeName)) themeName = 'tactical-midnight';
  currentTheme = themeName;
  document.documentElement.setAttribute('data-theme', themeName);
  localStorage.setItem('drs_theme', themeName);

  // Update button active states in both login and header
  document.querySelectorAll('.theme-btn').forEach(btn => {
    btn.classList.remove('active');
    if (btn.getAttribute('onclick')?.includes(themeName)) {
      btn.classList.add('active');
    }
  });

  // Re-render topology graphs to match palette
  if (allRoads.length > 0) {
    renderNetworkGraph('dashboard-network-svg');
    renderNetworkGraph('roads-network-svg');
  }
}

// DOM Ready initialization
document.addEventListener('DOMContentLoaded', () => {
  setAppTheme(currentTheme);
  if (authToken) {
    checkCurrentUser();
  } else {
    showLogin();
  }
});

// ---------------------------------------------------------------------------------------------
// AUTHENTICATION & SESSIONS
// ---------------------------------------------------------------------------------------------

function quickFill(userType) {
  const usernameInput = document.getElementById('login-username');
  const passwordInput = document.getElementById('login-password');
  const adminCard = document.getElementById('card-persona-admin');
  const officerCard = document.getElementById('card-persona-officer');

  if (userType === 'admin') {
    usernameInput.value = 'admin';
    passwordInput.value = 'Admin@12345';
    adminCard?.classList.add('active');
    officerCard?.classList.remove('active');
  } else {
    usernameInput.value = 'officer1';
    passwordInput.value = 'Officer@12345';
    officerCard?.classList.add('active');
    adminCard?.classList.remove('active');
  }
}

async function handleLoginSubmit(event) {
  event.preventDefault();
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value.trim();

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();

    if (!res.ok) {
      showToast(data.error || 'Login failed', 'danger');
      return;
    }

    authToken = data.token;
    currentUser = data.user;
    localStorage.setItem('drs_token', authToken);

    showToast(`Welcome back, ${currentUser.username}!`, 'success');
    setupUserInterface();
  } catch (err) {
    showToast('Failed to connect to server: ' + err.message, 'danger');
  }
}

async function checkCurrentUser() {
  try {
    const data = await fetchApi('/api/auth/me');
    currentUser = data;
    setupUserInterface();
  } catch (err) {
    handleLogout();
  }
}

function handleLogout() {
  if (authToken) {
    fetch('/api/auth/logout', {
      method: 'POST',
      headers: { 'X-Auth-Token': authToken }
    }).catch(() => {});
  }
  authToken = null;
  currentUser = null;
  localStorage.removeItem('drs_token');
  showLogin();
}

function showLogin() {
  document.getElementById('login-screen').style.display = 'flex';
  document.getElementById('app-screen').style.display = 'none';
}

function setupUserInterface() {
  document.getElementById('login-screen').style.display = 'none';
  document.getElementById('app-screen').style.display = 'flex';

  document.getElementById('user-display-name').textContent = currentUser.username;
  const roleBadge = document.getElementById('user-display-role');
  roleBadge.textContent = currentUser.role;

  const controlMenu = document.querySelector('.menu-role-control');
  const officerMenu = document.querySelector('.menu-role-officer');

  if (currentUser.role === 'CONTROL_ROOM') {
    roleBadge.className = 'role-pill role-control';
    controlMenu.style.display = 'block';
    officerMenu.style.display = 'none';
    switchView('dashboard');
  } else {
    roleBadge.className = 'role-pill role-officer';
    controlMenu.style.display = 'none';
    officerMenu.style.display = 'block';
    switchView('officer-area');
  }

  loadInitialStaticData();
  populateRegionOptions();
  refreshAllData();
}

// ---------------------------------------------------------------------------------------------
// API CLIENT HELPER
// ---------------------------------------------------------------------------------------------

async function fetchApi(url, options = {}) {
  const headers = options.headers || {};
  if (authToken) {
    headers['X-Auth-Token'] = authToken;
  }
  if (!headers['Content-Type'] && options.body) {
    headers['Content-Type'] = 'application/json';
  }
  options.headers = headers;

  const res = await fetch(url, options);
  if (res.status === 401) {
    showToast('Session expired. Please sign in again.', 'warning');
    handleLogout();
    throw new Error('Unauthorized');
  }
  if (res.status === 403) {
    const err = await res.json().catch(() => ({}));
    showToast(err.error || 'Access Forbidden', 'danger');
    throw new Error('Forbidden');
  }

  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.error || 'Server request error');
  }
  return data;
}

// ---------------------------------------------------------------------------------------------
// NAVIGATION & VIEW SWITCHING
// ---------------------------------------------------------------------------------------------

function switchView(viewName) {
  // Update sidebar active class
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  const clicked = event?.currentTarget;
  if (clicked) clicked.classList.add('active');

  // Hide all panels
  document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active'));

  // Show target panel
  const target = document.getElementById('panel-' + viewName);
  if (target) {
    target.classList.add('active');
  }

  // Set topbar title
  const titles = {
    'dashboard': 'Control Dashboard',
    'inbox': 'Review Inbox',
    'queue': 'Live Priority Queue',
    'process': 'Process & Allocate Desk',
    'allocations': 'Allocations Desk',
    'roads': 'Road Network & Routing',
    'stock': 'Warehouse Stock & Ledger',
    'teams': 'Rescue Teams',
    'shelters': 'Evacuation Shelters',
    'regions': 'Region Priority Board',
    'users': 'Personnel Management',
    'audit': 'Audit Trail',
    'impact': 'Impact Metrics & SDGs',
    'design': 'System Architecture Notes',
    'officer-area': 'My Assigned Area Feed',
    'officer-new': 'New Incident Assessment',
    'officer-reports': 'My Submitted Reports',
    'officer-road': 'Report Road Hazard'
  };
  document.getElementById('view-title').textContent = titles[viewName] || 'Operations Console';

  // Trigger view-specific loads
  if (viewName === 'inbox') loadInbox();
  if (viewName === 'queue') loadQueue();
  if (viewName === 'process') loadProcessDesk();
  if (viewName === 'allocations') loadAllocations();
  if (viewName === 'roads') loadRoads();
  if (viewName === 'stock') loadStock();
  if (viewName === 'teams') loadTeams();
  if (viewName === 'shelters') loadShelters();
  if (viewName === 'regions') loadRegionBoard();
  if (viewName === 'users') loadUsers();
  if (viewName === 'audit') loadAudit();
  if (viewName === 'impact') loadImpact();
  if (viewName === 'officer-area') loadOfficerArea();
  if (viewName === 'officer-reports') loadOfficerReports();
  if (viewName === 'officer-new') initReportFormView();
}

// ---------------------------------------------------------------------------------------------
// DATA REFRESH & FETCHERS
// ---------------------------------------------------------------------------------------------

async function refreshAllData() {
  if (currentUser?.role === 'CONTROL_ROOM') {
    loadDashboardStats();
    loadInbox();
    loadQueue();
  } else {
    loadOfficerArea();
  }
}

async function loadInitialStaticData() {
  try {
    const roads = await fetchApi('/api/roads');
    allRoads = roads;
    populateNodeSelects();
    renderNetworkGraph('dashboard-network-svg');
    renderNetworkGraph('roads-network-svg');
  } catch (err) {}
}

async function loadDashboardStats() {
  try {
    const stats = await fetchApi('/api/dashboard/stats');

    const pendingCount = (stats.pendingReportsCount != null) ? stats.pendingReportsCount : (stats.countsByStatus.PENDING_REVIEW || 0);
    document.getElementById('stat-queue-count').textContent = stats.liveQueueCount;
    document.getElementById('stat-inbox-count').textContent = pendingCount;
    document.getElementById('stat-stock-summary').textContent = `${stats.totalStockMedicine} / ${stats.totalStockFood}`;
    document.getElementById('stat-teams-summary').textContent = `${stats.availableTeams} / ${stats.availableTeams + stats.busyTeams}`;
    document.getElementById('stat-shelter-summary').textContent = `${stats.shelterReserved} / ${stats.shelterCapacity}`;

    // Badges in sidebar
    document.getElementById('badge-inbox').textContent = pendingCount;
    document.getElementById('badge-queue').textContent = stats.liveQueueCount;

    // Escalation Banner
    const banner = document.getElementById('escalation-banner');
    const bText = document.getElementById('escalation-text');
    if (stats.escalationFlag.includes('LEVEL_2')) {
      banner.className = 'escalation-banner banner-critical';
      bText.textContent = 'CRITICAL ALERT: ' + stats.escalationFlag;
    } else if (stats.escalationFlag.includes('LEVEL_1')) {
      banner.className = 'escalation-banner banner-high';
      bText.textContent = 'HIGH ALERT: ' + stats.escalationFlag;
    } else {
      banner.className = 'escalation-banner banner-normal';
      bText.textContent = 'OPERATIONAL STATUS: NORMAL';
    }

    // Isolated regions alert pill
    const isoPill = document.getElementById('isolated-alert-pill');
    if (stats.isolatedRegions && stats.isolatedRegions.length > 0) {
      isoPill.style.display = 'block';
      isoPill.textContent = `⚠️ ISOLATED REGIONS: [${stats.isolatedRegions.join(', ')}]`;
    } else {
      isoPill.style.display = 'none';
    }

    // Stock integrity status
    const intCard = document.getElementById('stat-integrity-status');
    if (stats.stockIntegrity && stats.stockIntegrity.valid) {
      intCard.textContent = '100% BALANCED';
      intCard.style.color = 'var(--success)';
    } else {
      intCard.textContent = 'MISMATCH DETECTED';
      intCard.style.color = 'var(--danger)';
    }

    // Render interactive topological graph
    renderNetworkGraph('dashboard-network-svg');

    // Dashboard quick queue table
    loadDashboardQueue();
  } catch (err) {
    console.error(err);
  }
}

async function loadDashboardQueue() {
  try {
    const queue = await fetchApi('/api/requests/queue');
    const tbody = document.querySelector('#table-dashboard-queue tbody');
    tbody.innerHTML = '';

    if (queue.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-dim);">No requests currently waiting in triage queue.</td></tr>';
      return;
    }

    queue.slice(0, 5).forEach(item => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong style="color: #60a5fa;">#${item.rank}</strong></td>
        <td><code>${item.requestId}</code></td>
        <td>${item.regionName}</td>
        <td><span class="badge-sev sev-badge-${item.severity}">${item.severity}</span></td>
        <td>${item.affectedPopulation.toLocaleString()}</td>
        <td><span class="badge-status status-waiting">${item.type}</span></td>
        <td style="font-size: 12px; color: var(--text-muted);">${item.whyThisRank}</td>
        <td>
          <button class="btn btn-primary btn-sm" onclick="previewAndOpenProcess('${item.requestId}')">Process</button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

async function loadInbox() {
  try {
    const inbox = await fetchApi('/api/reports/pending');
    const tbody = document.querySelector('#table-inbox tbody');
    tbody.innerHTML = '';

    if (!inbox || inbox.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-dim); padding: 24px;">No field incident assessments pending review.</td></tr>';
      return;
    }

    inbox.forEach(item => {
      const tr = document.createElement('tr');
      const counts = item.counts || {};
      const sev = item.severity || {};
      const effectiveLevel = (sev.overrideLevel != null) ? sev.overrideLevel : (sev.calculated || 3);

      let needsSummary = '';
      if (item.needOrder && item.needOrder.length > 0) {
        needsSummary = item.needOrder.map((n, i) => {
          let badgeColor = '#60a5fa';
          if (n === 'RESCUE') badgeColor = '#ef4444';
          if (n === 'SHELTER') badgeColor = '#c084fc';
          if (n === 'MEDICINE') badgeColor = '#34d399';
          if (n === 'FOOD') badgeColor = '#fbbf24';
          return `<span style="font-size: 10px; font-weight: 700; background: rgba(255,255,255,0.06); border: 1px solid ${badgeColor}; color: ${badgeColor}; padding: 2px 6px; border-radius: 4px; margin-right: 4px;">#${i+1} ${n}</span>`;
        }).join('');
      } else {
        needsSummary = '<span style="color: var(--text-dim);">-</span>';
      }

      const roadStatus = item.roadAccessObserved || 'UNKNOWN';
      let roadPill = `<span class="badge-status status-waiting">${roadStatus}</span>`;
      if (roadStatus === 'OPEN') roadPill = `<span class="badge-status status-allocated">OPEN</span>`;
      if (roadStatus === 'BLOCKED') roadPill = `<span class="badge-status status-blocked">BLOCKED</span>`;

      tr.innerHTML = `
        <td><strong style="color: #60a5fa; font-family: var(--font-mono);">${item.id}</strong></td>
        <td><strong>Region ${item.regionId}</strong></td>
        <td><small>${item.officerId || '-'}</small></td>
        <td>
          <div style="font-size: 12px; line-height: 1.4;">
            <span class="color-dot dot-red"></span><strong>${counts.red || 0}</strong> Red &bull;
            <span class="color-dot dot-yellow"></span><strong>${counts.yellow || 0}</strong> Ylw &bull;
            <span class="color-dot dot-green"></span><strong>${counts.green || 0}</strong> Grn &bull;
            <span class="color-dot dot-black"></span><strong>${counts.black || 0}</strong> Blk
            <div style="font-size: 11px; color: var(--text-muted); margin-top: 2px;">
              Trap: ${counts.trapped || 0} | Vuln: ${counts.vulnerable || 0} | Total: ${(counts.total || 0).toLocaleString()}
            </div>
          </div>
        </td>
        <td>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span class="badge-sev sev-badge-${effectiveLevel}">${effectiveLevel}</span>
            <div>
              <strong style="font-size: 11px; display: block;">${getSeverityLabel(effectiveLevel)}</strong>
              <small style="color: var(--text-muted); font-size: 10px; font-family: var(--font-mono);">${sev.ruleId || ''}</small>
            </div>
          </div>
        </td>
        <td>${needsSummary}</td>
        <td>${roadPill}</td>
        <td>
          <div style="display: flex; gap: 6px; flex-wrap: wrap;">
            <button class="btn btn-success btn-sm" onclick="handleApproveReport('${item.id}')" title="Approve &amp; create priority queue requests">Approve</button>
            <button class="btn btn-warning btn-sm" onclick="openReportOverrideModal('${item.id}', ${effectiveLevel}, '${sev.ruleId || ''}')" title="Override severity level">Override</button>
            <button class="btn btn-danger btn-sm" onclick="openReportRejectModal('${item.id}')" title="Reject assessment">Reject</button>
            <button class="btn btn-secondary btn-sm" onclick="viewReportDetails('${item.id}')" title="View complete report">Details</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error('loadInbox error:', err);
  }
}

async function handleApproveReport(reportId) {
  try {
    const res = await fetchApi(`/api/reports/${reportId}/approve`, { method: 'POST' });
    showToast(`Report ${reportId} approved! Enqueued linked requests: ${(res.linkedRequestIds || []).join(', ')}`, 'success');
    loadInbox();
    loadDashboardStats();
    loadQueue();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function openReportOverrideModal(reportId, currentLevel, ruleId = '') {
  document.getElementById('override-rep-id').value = reportId;
  document.getElementById('override-rep-id-display').textContent = reportId;
  const badge = document.getElementById('override-rep-curr-display');
  badge.textContent = currentLevel;
  badge.className = `badge-sev sev-badge-${currentLevel}`;
  document.getElementById('override-rep-rule-display').textContent = ruleId ? `(${ruleId})` : '';
  document.getElementById('override-rep-new-level').value = currentLevel;
  document.getElementById('override-rep-reason').value = '';
  openModal('modal-override-report');
}

async function handleReportOverrideSubmit(event) {
  event.preventDefault();
  const reportId = document.getElementById('override-rep-id').value;
  const newLevel = parseInt(document.getElementById('override-rep-new-level').value);
  const reason = document.getElementById('override-rep-reason').value.trim();

  if (!reason) {
    showToast('Justification reason is mandatory for severity override', 'warning');
    return;
  }

  try {
    await fetchApi(`/api/reports/${reportId}/override`, {
      method: 'POST',
      body: JSON.stringify({ newLevel, reason })
    });
    showToast(`Report ${reportId} severity overridden to Level ${newLevel}!`, 'success');
    closeModal('modal-override-report');
    loadInbox();
    loadDashboardStats();
    loadQueue();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function openReportRejectModal(reportId) {
  document.getElementById('reject-rep-id').value = reportId;
  document.getElementById('reject-rep-id-display').textContent = reportId;
  document.getElementById('reject-rep-reason').value = '';
  openModal('modal-reject-report');
}

async function handleReportRejectSubmit(event) {
  event.preventDefault();
  const reportId = document.getElementById('reject-rep-id').value;
  const reason = document.getElementById('reject-rep-reason').value.trim();

  if (!reason) {
    showToast('Rejection reason is mandatory', 'warning');
    return;
  }

  try {
    await fetchApi(`/api/reports/${reportId}/reject`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    });
    showToast(`Report ${reportId} rejected.`, 'info');
    closeModal('modal-reject-report');
    loadInbox();
    loadDashboardStats();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function getSeverityLabel(level) {
  switch (level) {
    case 5: return 'CATASTROPHIC';
    case 4: return 'SEVERE EMERGENCY';
    case 3: return 'MODERATE DISTRESS';
    case 2: return 'MINOR INCIDENTS';
    case 1: return 'LOW PRIORITY';
    default: return 'LEVEL ' + level;
  }
}

// Legacy Request Review Support
async function reviewRequest(requestId, decision, reason = '', newSeverity = 0) {
  try {
    await fetchApi('/api/requests/review', {
      method: 'POST',
      body: JSON.stringify({ requestId, decision, reason, newSeverity })
    });
    showToast(`Request ${requestId} marked as ${decision}`, 'success');
    loadInbox();
    loadDashboardStats();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

function openOverrideModal(requestId, currentSeverity) {
  document.getElementById('override-request-id').value = requestId;
  document.getElementById('override-new-severity').value = currentSeverity;
  document.getElementById('override-reason').value = '';
  openModal('modal-override-severity');
}

async function handleOverrideSubmit(event) {
  event.preventDefault();
  const requestId = document.getElementById('override-request-id').value;
  const newSeverity = parseInt(document.getElementById('override-new-severity').value);
  const reason = document.getElementById('override-reason').value.trim();

  await reviewRequest(requestId, 'OVERRIDE', reason, newSeverity);
  closeModal('modal-override-severity');
}

async function loadQueue() {
  try {
    const queue = await fetchApi('/api/requests/queue');
    const tbody = document.querySelector('#table-queue-full tbody');
    tbody.innerHTML = '';

    if (queue.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-dim);">Triage queue is empty.</td></tr>';
      return;
    }

    queue.forEach(item => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong style="color: #60a5fa;">#${item.rank}</strong></td>
        <td><code>${item.requestId}</code></td>
        <td>${item.regionName}</td>
        <td><span class="badge-sev sev-badge-${item.severity}">${item.severity}</span></td>
        <td>${item.affectedPopulation.toLocaleString()}</td>
        <td><code>#${item.arrivalNo}</code></td>
        <td style="color: #93c5fd; font-weight: 500;">${item.whyThisRank}</td>
        <td><span class="badge-status status-waiting">${item.status}</span></td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="cancelRequest('${item.requestId}')">Cancel</button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

async function cancelRequest(id) {
  if (!confirm(`Cancel request ${id}?`)) return;
  try {
    await fetchApi('/api/requests/' + id, { method: 'DELETE' });
    showToast(`Request ${id} cancelled`, 'success');
    loadQueue();
    loadDashboardStats();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

// ---------------------------------------------------------------------------------------------
// PROCESS DESK (PREVIEW & COMMIT WORKFLOW)
// ---------------------------------------------------------------------------------------------

async function loadProcessDesk() {
  try {
    const queue = await fetchApi('/api/requests/queue');
    const select = document.getElementById('select-process-request');
    select.innerHTML = '';

    if (queue.length === 0) {
      select.innerHTML = '<option value="">-- No waiting requests in queue --</option>';
      document.getElementById('btn-commit-selected').disabled = true;
      return;
    }

    queue.forEach(item => {
      const opt = document.createElement('option');
      opt.value = item.requestId;
      opt.textContent = `Rank #${item.rank} [${item.requestId}] ${item.regionName} - Severity ${item.severity} (${item.type})`;
      select.appendChild(opt);
    });

    if (queue.length > 0) {
      handleSelectProcessRequest(queue[0].requestId);
    }
  } catch (err) {}
}

function handleSelectProcessRequest(reqId) {
  currentPreviewRequestId = reqId;
  document.getElementById('preview-result-container').style.display = 'none';
  document.getElementById('btn-commit-selected').disabled = true;
}

function previewAndOpenProcess(reqId) {
  switchView('process');
  setTimeout(() => {
    document.getElementById('select-process-request').value = reqId;
    handleSelectProcessRequest(reqId);
    executePreviewForSelected();
  }, 100);
}

async function executePreviewForSelected() {
  const reqId = document.getElementById('select-process-request').value;
  if (!reqId) return;

  try {
    const preview = await fetchApi('/api/allocations/preview/' + reqId);
    currentPreviewRequestId = reqId;

    const container = document.getElementById('preview-result-container');
    const pill = document.getElementById('preview-status-pill');
    const body = document.getElementById('preview-details-body');
    container.style.display = 'block';

    if (!preview.reachable) {
      pill.innerHTML = '<span class="badge-status status-blocked">UNREACHABLE (BLOCKED)</span>';
      body.innerHTML = `<div style="color: #f87171; font-weight: 600;">${preview.statusMessage}</div>
                        <p style="margin-top: 8px;">Road network Dijkstra traversal reported no viable open path.</p>`;
      document.getElementById('btn-commit-selected').disabled = true;
      return;
    }

    if (!preview.feasible) {
      pill.innerHTML = '<span class="badge-status status-waiting">UNFEASIBLE (DEFERRED)</span>';
      body.innerHTML = `<div style="color: #fbbf24; font-weight: 600;">${preview.statusMessage}</div>`;
      document.getElementById('btn-commit-selected').disabled = true;
      return;
    }

    pill.innerHTML = '<span class="badge-status status-allocated">FEASIBLE & READY TO COMMIT</span>';
    document.getElementById('btn-commit-selected').disabled = false;

    if (preview.requestType === 'SUPPLY') {
      let plansHtml = preview.depotPlans.map(dp => `
        <div style="background: rgba(31, 41, 55, 0.6); padding: 10px 14px; border-radius: 6px; margin-top: 8px;">
          <strong>Depot:</strong> ${dp.depotName} (Node ${dp.depotNode}) &bull;
          <strong>Travel Time:</strong> ${dp.travelTime} min<br>
          <strong>Drawn:</strong> ${dp.medicineDrawn} medicine kits, ${dp.foodDrawn} food packs<br>
          <strong>Dijkstra Route:</strong> <code>${dp.route.join(' &rarr; ')}</code>
        </div>
      `).join('');

      body.innerHTML = `
        <div><strong>Target:</strong> ${preview.targetRegionName} (Node ${preview.targetRegionId})</div>
        <div><strong>Requested:</strong> ${preview.medicineNeeded} Medicine, ${preview.foodNeeded} Food</div>
        <div><strong>Allocated:</strong> ${preview.medicineAllocated} Medicine, ${preview.foodAllocated} Food</div>
        ${preview.outstandingMedicine > 0 || preview.outstandingFood > 0 ?
          `<div style="color: #f59e0b; margin-top: 6px;">⚠️ Outstanding Shortfall: ${preview.outstandingMedicine} medicine, ${preview.outstandingFood} food (will mark PARTIALLY_ALLOCATED)</div>` : ''}
        <h4 style="margin-top: 14px; font-size: 13px;">Selected Depots:</h4>
        ${plansHtml}
      `;
      if (preview.depotPlans.length > 0 && preview.depotPlans[0].route) {
        renderNetworkGraph('dashboard-network-svg', preview.depotPlans[0].route);
      }
    } else {
      body.innerHTML = `
        <div><strong>Target:</strong> ${preview.targetRegionName} (Node ${preview.targetRegionId})</div>
        <div><strong>Assigned Team:</strong> ${preview.teamName} (Travel: ${preview.teamTravelTime} min)</div>
        <div><strong>Team Route:</strong> <code>${preview.teamRoute.join(' &rarr; ')}</code></div>
        <div style="margin-top: 8px;"><strong>Assigned Shelter:</strong> ${preview.shelterName} (Travel: ${preview.shelterTravelTime} min)</div>
        <div><strong>Evacuated Persons:</strong> ${preview.peopleEvacuated} of ${preview.peopleToEvacuate}</div>
        ${preview.peopleUnsheltered > 0 ? `<div style="color: #f59e0b;">⚠️ ${preview.peopleUnsheltered} people could not be sheltered due to capacity</div>` : ''}
      `;
      if (preview.teamRoute) {
        renderNetworkGraph('dashboard-network-svg', preview.teamRoute);
      }
    }
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function executeCommitForSelected() {
  if (!currentPreviewRequestId) return;
  try {
    const res = await fetchApi('/api/allocations/commit', {
      method: 'POST',
      body: JSON.stringify({ requestId: currentPreviewRequestId })
    });
    if (res.success) {
      showToast(`Allocation successfully committed for ${currentPreviewRequestId}!`, 'success');
      loadProcessDesk();
      loadDashboardStats();
    } else {
      showToast('Commit failed or request was deferred/blocked.', 'warning');
    }
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function processNextDirect() {
  try {
    const res = await fetchApi('/api/allocations/process-next', { method: 'POST' });
    if (res.success) {
      showToast(`Processed top request: ${res.processedRequestId}`, 'success');
      refreshAllData();
    } else {
      showToast(res.message || 'No requests processed', 'warning');
    }
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function processAllDirect() {
  try {
    const res = await fetchApi('/api/allocations/process-all', { method: 'POST' });
    showToast(`Bulk processed ${res.processedCount} requests successfully`, 'success');
    refreshAllData();
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

// ---------------------------------------------------------------------------------------------
// ALLOCATIONS & MISSIONS
// ---------------------------------------------------------------------------------------------

async function loadAllocations() {
  try {
    const allocs = await fetchApi('/api/allocations');
    const tbody = document.querySelector('#table-allocations tbody');
    tbody.innerHTML = '';

    if (allocs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: var(--text-dim);">No allocations recorded yet.</td></tr>';
      return;
    }

    allocs.forEach(a => {
      const tr = document.createElement('tr');
      const asset = a.type === 'SUPPLY' ? `Depot: ${a.depotId}` : `Team: ${a.teamId} / Shelter: ${a.shelterId}`;
      const units = a.type === 'SUPPLY' ? `Med: ${a.medicineAllocated}, Food: ${a.foodAllocated}` : `Evacuated: ${a.peopleEvacuated}`;
      tr.innerHTML = `
        <td><code>${a.id}</code></td>
        <td><code>${a.requestId}</code></td>
        <td>${a.type}</td>
        <td>Region ${a.targetRegionId}</td>
        <td><small>${asset}</small></td>
        <td>${units}</td>
        <td>${a.travelTime} min</td>
        <td><span class="badge-status status-${a.status.toLowerCase()}">${a.status}</span></td>
        <td>
          <div style="display: flex; gap: 6px;">
            ${a.status === 'COMMITTED' ? `<button class="btn btn-primary btn-sm" onclick="dispatchAllocation('${a.id}')">Dispatch</button>` : ''}
            ${a.status === 'DISPATCHED' ? `<button class="btn btn-success btn-sm" onclick="deliverAllocation('${a.id}')">Confirm Delivered</button>` : ''}
            ${a.status === 'COMMITTED' ? `<button class="btn btn-danger btn-sm" onclick="cancelAllocation('${a.id}')">Cancel</button>` : ''}
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

async function dispatchAllocation(id) {
  try {
    await fetchApi(`/api/allocations/${id}/dispatch`, { method: 'POST' });
    showToast(`Mission ${id} marked DISPATCHED into the field`, 'info');
    loadAllocations();
  } catch (err) { showToast(err.message, 'danger'); }
}

async function deliverAllocation(id) {
  try {
    await fetchApi(`/api/allocations/${id}/deliver`, { method: 'POST' });
    showToast(`Delivery confirmed for mission ${id}! Resources consumed.`, 'success');
    loadAllocations();
    loadDashboardStats();
  } catch (err) { showToast(err.message, 'danger'); }
}

async function cancelAllocation(id) {
  if (!confirm(`Cancel allocation ${id}? Reserved stock/places will be returned.`)) return;
  try {
    await fetchApi(`/api/allocations/${id}/cancel`, { method: 'POST' });
    showToast(`Allocation ${id} cancelled and resources released`, 'warning');
    loadAllocations();
    loadDashboardStats();
  } catch (err) { showToast(err.message, 'danger'); }
}

// ---------------------------------------------------------------------------------------------
// ROADS & NETWORK GRAPH (WITH INTERACTIVE SVG VISUALIZER)
// ---------------------------------------------------------------------------------------------

const NODE_LAYOUT = {
  1:  { x: 160, y: 190, name: 'LOC01: Aruvi Central (Control Room)', type: 'region', badge: 'LOC01' },
  2:  { x: 290, y: 120, name: 'LOC02: Northbank', type: 'region', badge: 'LOC02' },
  3:  { x: 440, y: 80,  name: 'LOC03: Hillview', type: 'region', badge: 'LOC03' },
  4:  { x: 280, y: 260, name: 'LOC04: Riverside', type: 'region', badge: 'LOC04' },
  5:  { x: 420, y: 210, name: 'LOC05: Greenfield', type: 'region', badge: 'LOC05' },
  6:  { x: 570, y: 150, name: 'LOC06: Eastwood', type: 'region', badge: 'LOC06' },
  7:  { x: 210, y: 340, name: 'LOC07: Lakeview', type: 'region', badge: 'LOC07' },
  8:  { x: 690, y: 120, name: 'LOC08: Pinebridge', type: 'region', badge: 'LOC08' },
  9:  { x: 500, y: 320, name: 'LOC09: Meadowpark', type: 'region', badge: 'LOC09' },
  10: { x: 740, y: 260, name: 'LOC10: Stonepass', type: 'region', badge: 'LOC10' },
  11: { x: 100, y: 140, name: 'DEP01: Aruvi Central Depot', type: 'depot', badge: 'DEP01' },
  12: { x: 240, y: 60,  name: 'DEP02: Northbank Regional Depot', type: 'depot', badge: 'DEP02' },
  13: { x: 100, y: 250, name: 'SH01: Aruvi Relief Centre', type: 'shelter', badge: 'SH01' },
  14: { x: 350, y: 60,  name: 'SH02: Northbank Community Shelter', type: 'shelter', badge: 'SH02' }
};

let activeHighlightedPath = [];

function renderNetworkGraph(svgId, highlightedPathNodes = []) {
  const svg = document.getElementById(svgId);
  if (!svg) return;
  if (highlightedPathNodes && highlightedPathNodes.length > 0) {
    activeHighlightedPath = highlightedPathNodes;
  }

  svg.innerHTML = '';

  // Defs & glow filters
  const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
  defs.innerHTML = `
    <pattern id="grid-pat-${svgId}" width="40" height="40" patternUnits="userSpaceOnUse">
      <path d="M 40 0 L 0 0 0 40" fill="none" stroke="rgba(255,255,255,0.03)" stroke-width="1"/>
    </pattern>
    <filter id="glow-gold-${svgId}" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="5" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
  `;
  svg.appendChild(defs);

  const bgRect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
  bgRect.setAttribute('width', '100%');
  bgRect.setAttribute('height', '100%');
  bgRect.setAttribute('fill', `url(#grid-pat-${svgId})`);
  svg.appendChild(bgRect);

  const edgeGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
  const nodeGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
  svg.appendChild(edgeGroup);
  svg.appendChild(nodeGroup);

  // 1. Draw Roads
  allRoads.forEach(road => {
    const fromCoord = NODE_LAYOUT[road.from];
    const toCoord = NODE_LAYOUT[road.to];
    if (!fromCoord || !toCoord) return;

    const isClosed = road.status === 'CLOSED';
    let isPathEdge = false;
    if (activeHighlightedPath.length >= 2) {
      for (let i = 0; i < activeHighlightedPath.length - 1; i++) {
        const u = activeHighlightedPath[i];
        const v = activeHighlightedPath[i+1];
        if ((road.from === u && road.to === v) || (road.from === v && road.to === u)) {
          isPathEdge = true;
          break;
        }
      }
    }

    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    line.setAttribute('x1', fromCoord.x);
    line.setAttribute('y1', fromCoord.y);
    line.setAttribute('x2', toCoord.x);
    line.setAttribute('y2', toCoord.y);
    
    let edgeClasses = 'graph-edge';
    if (isClosed) edgeClasses += ' edge-blocked';
    if (isPathEdge) edgeClasses += ' edge-active-route';
    line.setAttribute('class', edgeClasses);

    line.addEventListener('click', () => {
      if (isClosed) {
        toggleRoadStatus(road.id, 'OPEN');
      } else {
        promptCloseRoad(road.id);
      }
    });

    const title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
    title.textContent = `Road ${road.id}: Node ${road.from} ↔ Node ${road.to} (${road.travelTime} min) - ${road.status}. Click to toggle!`;
    line.appendChild(title);
    edgeGroup.appendChild(line);

    // Label on road midpoint
    const midX = (fromCoord.x + toCoord.x) / 2;
    const midY = (fromCoord.y + toCoord.y) / 2;

    const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    text.setAttribute('x', midX);
    text.setAttribute('y', midY - 5);
    text.setAttribute('class', 'graph-edge-text');
    if (isClosed) {
      text.textContent = `⚠️ BLOCKED`;
      text.setAttribute('fill', '#ef4444');
      text.setAttribute('font-weight', '700');
    } else {
      text.textContent = `${road.travelTime}m`;
    }
    edgeGroup.appendChild(text);
  });

  // 2. Draw Nodes
  Object.keys(NODE_LAYOUT).forEach(nodeKey => {
    const id = parseInt(nodeKey);
    const n = NODE_LAYOUT[id];

    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    g.setAttribute('transform', `translate(${n.x}, ${n.y})`);

    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    let radius = 18;
    let strokeColor = '#38bdf8';
    let fillColor = 'rgba(14, 165, 233, 0.15)';

    if (n.type === 'depot') {
      radius = 21;
      strokeColor = '#10b981';
      fillColor = 'rgba(16, 185, 129, 0.25)';
    } else if (n.type === 'shelter') {
      radius = 20;
      strokeColor = '#a855f7';
      fillColor = 'rgba(168, 85, 247, 0.25)';
    }

    if (activeHighlightedPath.includes(id)) {
      strokeColor = '#eab308';
      fillColor = 'rgba(234, 179, 8, 0.35)';
      circle.setAttribute('filter', `url(#glow-gold-${svgId})`);
    }

    circle.setAttribute('r', radius);
    circle.setAttribute('class', 'graph-node-circle');
    circle.setAttribute('stroke', strokeColor);
    circle.setAttribute('fill', fillColor);

    circle.addEventListener('click', () => {
      const srcEl = document.getElementById('route-source');
      const dstEl = document.getElementById('route-target');
      if (srcEl && (!srcEl.value || srcEl.value === '1')) {
        srcEl.value = id;
        showToast(`Route Source set to Node ${id} (${n.name})`, 'info');
      } else if (dstEl) {
        dstEl.value = id;
        showToast(`Route Destination set to Node ${id} (${n.name})`, 'info');
      }
    });

    const textInside = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    textInside.setAttribute('y', 4);
    textInside.setAttribute('class', 'graph-node-text');
    textInside.setAttribute('font-size', '11px');
    textInside.textContent = n.badge;

    const textBelow = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    textBelow.setAttribute('y', radius + 14);
    textBelow.setAttribute('class', 'graph-node-text');
    textBelow.setAttribute('font-size', '10px');
    textBelow.setAttribute('fill', 'var(--text-muted)');
    textBelow.textContent = n.name;

    const tooltip = document.createElementNS('http://www.w3.org/2000/svg', 'title');
    tooltip.textContent = `Node ${id}: ${n.name} (${n.type.toUpperCase()}). Click to select in Route Finder.`;
    g.appendChild(tooltip);

    g.appendChild(circle);
    g.appendChild(textInside);
    g.appendChild(textBelow);
    nodeGroup.appendChild(g);
  });
}

async function loadRoads() {
  try {
    const roads = await fetchApi('/api/roads');
    allRoads = roads;
    const tbody = document.querySelector('#table-roads tbody');
    tbody.innerHTML = '';

    roads.forEach(r => {
      const tr = document.createElement('tr');
      const isClosed = r.status === 'CLOSED';
      tr.innerHTML = `
        <td><code>${r.id}</code></td>
        <td>Node ${r.from} &harr; Node ${r.to}</td>
        <td>${r.travelTime} min</td>
        <td><span class="badge-status ${isClosed ? 'status-blocked' : 'status-allocated'}">${r.status}</span></td>
        <td style="color: ${isClosed ? '#f87171' : 'var(--text-dim)'};">${r.closureReason || '-'}</td>
        <td>
          ${isClosed ?
            `<button class="btn btn-success btn-sm" onclick="toggleRoadStatus('${r.id}', 'OPEN')">Reopen Road</button>` :
            `<button class="btn btn-danger btn-sm" onclick="promptCloseRoad('${r.id}')">Close Road</button>`}
        </td>
      `;
      tbody.appendChild(tr);
    });

    renderNetworkGraph('roads-network-svg');
  } catch (err) {}
}

function promptCloseRoad(roadId) {
  const reason = prompt('Enter closure reason (e.g. Flash flood, landslide, bridge collapse):', 'Severe storm damage');
  if (reason) {
    toggleRoadStatus(roadId, 'CLOSED', reason);
  }
}

async function toggleRoadStatus(roadId, status, reason = '') {
  try {
    await fetchApi(`/api/roads/${roadId}/status`, {
      method: 'POST',
      body: JSON.stringify({ status, reason })
    });
    showToast(`Road ${roadId} is now ${status}. Live graph rebuilt.`, 'info');
    loadRoads();
    loadDashboardStats();
    renderNetworkGraph('dashboard-network-svg');
    renderNetworkGraph('roads-network-svg');
  } catch (err) { showToast(err.message, 'danger'); }
}

async function calculateRouteDijkstra() {
  const source = parseInt(document.getElementById('route-source').value);
  const target = parseInt(document.getElementById('route-target').value);

  try {
    const res = await fetchApi('/api/roads/route', {
      method: 'POST',
      body: JSON.stringify({ source, target })
    });
    const resultBox = document.getElementById('route-calc-result');
    resultBox.style.display = 'block';

    if (res.reachable) {
      resultBox.innerHTML = `
        <div style="color: #34d399; font-weight: 700; margin-bottom: 6px;">✓ Route Reachable (Dijkstra Shortest Path)</div>
        <div><strong>Total Travel Time:</strong> ${res.distance} minutes</div>
        <div style="margin-top: 4px;"><strong>Path:</strong> <code>${res.path.join(' &rarr; ')}</code></div>
      `;
      renderNetworkGraph('roads-network-svg', res.path);
    } else {
      resultBox.innerHTML = `
        <div style="color: #f87171; font-weight: 700;">✗ No Viable Path (Unreachable)</div>
        <div style="color: var(--text-muted); margin-top: 4px;">Destination cannot be reached due to closed roads.</div>
      `;
      renderNetworkGraph('roads-network-svg', []);
    }
  } catch (err) { showToast(err.message, 'danger'); }
}

function populateNodeSelects() {
  const nodes = [
    { id: 1, name: 'LOC01 - Aruvi Central (District Control Room)' },
    { id: 2, name: 'LOC02 - Northbank' },
    { id: 3, name: 'LOC03 - Hillview' },
    { id: 4, name: 'LOC04 - Riverside' },
    { id: 5, name: 'LOC05 - Greenfield' },
    { id: 6, name: 'LOC06 - Eastwood' },
    { id: 7, name: 'LOC07 - Lakeview' },
    { id: 8, name: 'LOC08 - Pinebridge' },
    { id: 9, name: 'LOC09 - Meadowpark' },
    { id: 10, name: 'LOC10 - Stonepass' },
    { id: 11, name: 'DEP01 - Aruvi Central Depot (LOC01)' },
    { id: 12, name: 'DEP02 - Northbank Regional Depot (LOC02)' },
    { id: 13, name: 'SH01 - Aruvi Relief Centre (LOC01)' },
    { id: 14, name: 'SH02 - Northbank Community Shelter (LOC02)' }
  ];

  const s1 = document.getElementById('route-source');
  const s2 = document.getElementById('route-target');
  const sReg = document.getElementById('modal-req-region');
  const sRepReg = document.getElementById('report-region-select') || document.getElementById('report-region');
  const sHazRoad = document.getElementById('hazard-road');

  if (s1 && s2) {
    s1.innerHTML = '';
    s2.innerHTML = '';
    nodes.forEach(n => {
      s1.innerHTML += `<option value="${n.id}">${n.name}</option>`;
      s2.innerHTML += `<option value="${n.id}">${n.name}</option>`;
    });
    s1.value = 11; // Depot 1
    s2.value = 4;  // Village 4
  }

  if (sReg) {
    sReg.innerHTML = '';
    nodes.slice(0, 10).forEach(n => sReg.innerHTML += `<option value="${n.id}">${n.name}</option>`);
  }
  if (sRepReg) {
    populateRegionOptions();
  }
  if (sHazRoad && allRoads.length > 0) {
    sHazRoad.innerHTML = '';
    allRoads.forEach(r => sHazRoad.innerHTML += `<option value="${r.id}">${r.id}: Node ${r.from} &harr; ${r.to} (${r.travelTime}m)</option>`);
  }
}

// ---------------------------------------------------------------------------------------------
// STOCK & LEDGERS
// ---------------------------------------------------------------------------------------------

async function loadStock() {
  try {
    const depots = await fetchApi('/api/stock/depots');
    allDepots = depots;
    const container = document.getElementById('depots-cards-container');
    container.innerHTML = '';

    depots.forEach(d => {
      const card = document.createElement('div');
      card.className = 'stat-card';
      card.innerHTML = `
        <div class="stat-title">${d.name} (Node ${d.node})</div>
        <div style="margin-top: 10px; font-size: 13px;">
          <div>💊 <strong>Available Medicine:</strong> ${d.medicineKits} kits <small style="color: var(--text-dim);">(Reserved: ${d.reservedMedicine})</small></div>
          <div style="margin-top: 4px;">🍞 <strong>Available Food:</strong> ${d.foodPacks} packs <small style="color: var(--text-dim);">(Reserved: ${d.reservedFood})</small></div>
        </div>
      `;
      container.appendChild(card);
    });

    // Populate restock select
    const select = document.getElementById('restock-depot');
    select.innerHTML = '';
    depots.forEach(d => select.innerHTML += `<option value="${d.id}">${d.name}</option>`);

    // Load ledger
    const ledgers = await fetchApi('/api/stock/ledger');
    const tbody = document.querySelector('#table-ledger tbody');
    tbody.innerHTML = '';
    ledgers.slice(0, 15).forEach(l => {
      const tr = document.createElement('tr');
      const isPositive = l.delta > 0;
      tr.innerHTML = `
        <td><small>${new Date(l.timestamp).toLocaleTimeString()}</small></td>
        <td><code>${l.depotId}</code></td>
        <td>${l.item}</td>
        <td style="color: ${isPositive ? 'var(--success)' : '#f87171'}; font-weight: 700;">${isPositive ? '+' : ''}${l.delta}</td>
        <td><span class="badge-status status-allocated">${l.actionType}</span></td>
        <td><small>${l.user}</small></td>
        <td style="color: var(--text-muted); font-size: 12px;">${l.reason}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

function openRestockModal() {
  openModal('modal-restock');
}

async function handleRestockSubmit(event) {
  event.preventDefault();
  const depotId = document.getElementById('restock-depot').value;
  const medicineDelta = parseInt(document.getElementById('restock-med-delta').value);
  const foodDelta = parseInt(document.getElementById('restock-food-delta').value);
  const reason = document.getElementById('restock-reason').value.trim();

  try {
    await fetchApi('/api/stock/adjust', {
      method: 'POST',
      body: JSON.stringify({ depotId, medicineDelta, foodDelta, reason })
    });
    showToast(`Inventory updated for ${depotId}! Ledger entry recorded.`, 'success');
    closeModal('modal-restock');
    loadStock();
    loadDashboardStats();
  } catch (err) { showToast(err.message, 'danger'); }
}

// ---------------------------------------------------------------------------------------------
// TEAMS & SHELTERS
// ---------------------------------------------------------------------------------------------

async function loadTeams() {
  try {
    const teams = await fetchApi('/api/teams');
    const container = document.getElementById('teams-cards-container');
    container.innerHTML = '';

    teams.forEach(t => {
      const card = document.createElement('div');
      const isAvail = t.status === 'AVAILABLE';
      card.className = `stat-card ${isAvail ? 'success-card' : 'warning-card'}`;
      card.innerHTML = `
        <div class="stat-title">${t.name}</div>
        <div class="stat-value" style="font-size: 16px; margin-top: 8px;">Status: <span class="badge-status ${isAvail ? 'status-allocated' : 'status-waiting'}">${t.status}</span></div>
        <div class="stat-subtext">Base Station: Node ${t.baseNode}</div>
      `;
      container.appendChild(card);
    });
  } catch (err) {}
}

async function loadShelters() {
  try {
    const shelters = await fetchApi('/api/shelters');
    const container = document.getElementById('shelters-cards-container');
    container.innerHTML = '';

    shelters.forEach(s => {
      const card = document.createElement('div');
      const pct = Math.round((s.reserved / s.capacity) * 100);
      card.className = 'stat-card';
      card.innerHTML = `
        <div class="stat-title">${s.name} (Node ${s.node})</div>
        <div style="margin-top: 12px;">
          <div style="display: flex; justify-content: space-between; font-size: 13px; font-weight: 600;">
            <span>Occupancy: ${s.reserved} / ${s.capacity}</span>
            <span>${pct}%</span>
          </div>
          <div style="background: #374151; height: 8px; border-radius: 4px; overflow: hidden; margin-top: 6px;">
            <div style="background: ${pct >= 100 ? 'var(--danger)' : 'var(--primary)'}; height: 100%; width: ${pct}%;"></div>
          </div>
          <div class="stat-subtext" style="margin-top: 8px;">Available Slots: ${s.availableCapacity}</div>
        </div>
      `;
      container.appendChild(card);
    });
  } catch (err) {}
}

// ---------------------------------------------------------------------------------------------
// REGION BOARD & IMPACT
// ---------------------------------------------------------------------------------------------

async function loadRegionBoard() {
  try {
    const board = await fetchApi('/api/dashboard/regions');
    const tbody = document.querySelector('#table-region-board tbody');
    tbody.innerHTML = '';

    board.forEach(r => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>Node ${r.regionId}</td>
        <td><strong>${r.regionName}</strong></td>
        <td>${r.population.toLocaleString()}</td>
        <td>
          ${r.highestSeverity > 0 ?
            `<span class="badge-sev sev-badge-${r.highestSeverity}">${r.highestSeverity}</span>` :
            `<span style="color: var(--text-dim);">-</span>`}
        </td>
        <td><code>${r.waitingRequestId}</code></td>
        <td style="color: #93c5fd; font-size: 12px;">${r.priorityRankReason}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

async function loadImpact() {
  try {
    const impact = await fetchApi('/api/dashboard/impact');
    document.getElementById('impact-percent-fulfilled').textContent = impact.percentFulfilled + '%';
    document.getElementById('impact-sheltered').textContent = impact.peopleSheltered;
    document.getElementById('impact-kits').textContent = `${impact.medicineKitsDelivered} / ${impact.foodPacksDelivered}`;
    document.getElementById('impact-avg-time').textContent = impact.averageTravelTimeMinutes + ' min';

    const sdgBox = document.getElementById('sdg-container');
    sdgBox.innerHTML = '';
    for (const [title, desc] of Object.entries(impact.sdgAlignment || {})) {
      const div = document.createElement('div');
      div.style.marginBottom = '14px';
      div.innerHTML = `<h4 style="color: #60a5fa; font-size: 14px;">${title}</h4><p style="color: var(--text-muted); font-size: 13px;">${desc}</p>`;
      sdgBox.appendChild(div);
    }
  } catch (err) {}
}

// ---------------------------------------------------------------------------------------------
// PERSONNEL & AUDIT
// ---------------------------------------------------------------------------------------------

async function loadUsers() {
  try {
    const users = await fetchApi('/api/users');
    const tbody = document.querySelector('#table-users tbody');
    tbody.innerHTML = '';

    users.forEach(u => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><strong>${u.username}</strong></td>
        <td><span class="role-pill ${u.role === 'CONTROL_ROOM' ? 'role-control' : 'role-officer'}">${u.role}</span></td>
        <td>[${u.assignedLocationIds.join(', ')}]</td>
        <td><span class="badge-status ${u.active ? 'status-allocated' : 'status-blocked'}">${u.active ? 'ACTIVE' : 'DISABLED'}</span></td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="toggleUserActive('${u.id}', ${!u.active})">
            ${u.active ? 'Disable' : 'Enable'}
          </button>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

async function toggleUserActive(id, newStatus) {
  try {
    await fetchApi(`/api/users/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ active: newStatus })
    });
    showToast('User status updated', 'success');
    loadUsers();
  } catch (err) { showToast(err.message, 'danger'); }
}

async function handleCreateUserSubmit(event) {
  event.preventDefault();
  const username = document.getElementById('new-user-username').value.trim();
  const password = document.getElementById('new-user-password').value.trim();
  const regionsStr = document.getElementById('new-user-regions').value.trim();
  const assignedLocationIds = regionsStr.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n));

  try {
    await fetchApi('/api/users', {
      method: 'POST',
      body: JSON.stringify({
        username,
        password,
        role: 'FIELD_OFFICER',
        assignedLocationIds
      })
    });
    showToast(`Officer ${username} created!`, 'success');
    document.getElementById('new-user-username').value = '';
    document.getElementById('new-user-password').value = '';
    loadUsers();
  } catch (err) { showToast(err.message, 'danger'); }
}

async function loadAudit() {
  try {
    const logs = await fetchApi('/api/audit');
    const tbody = document.querySelector('#table-audit tbody');
    tbody.innerHTML = '';

    logs.slice(0, 25).forEach(l => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td><small>${new Date(l.timestamp).toLocaleTimeString()}</small></td>
        <td><strong>${l.user}</strong></td>
        <td><span class="role-pill ${l.role === 'CONTROL_ROOM' ? 'role-control' : 'role-officer'}">${l.role}</span></td>
        <td><code>${l.action}</code></td>
        <td style="color: var(--text-muted); font-size: 12px;">${l.details}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {}
}

// ---------------------------------------------------------------------------------------------
// FIELD OFFICER SPECIFIC ACTIONS
// ---------------------------------------------------------------------------------------------

async function loadOfficerArea() {
  try {
    const data = await fetchApi('/api/officer/my-area');
    const regionNames = {
      1: 'LOC01: Aruvi Central',
      2: 'LOC02: Northbank',
      3: 'LOC03: Hillview',
      4: 'LOC04: Riverside',
      5: 'LOC05: Greenfield',
      6: 'LOC06: Eastwood',
      7: 'LOC07: Lakeview',
      8: 'LOC08: Pinebridge',
      9: 'LOC09: Meadowpark',
      10: 'LOC10: Stonepass'
    };
    const badges = data.assignedRegions.map(id => regionNames[id] || ('LOC' + (id < 10 ? '0' + id : id)));
    document.getElementById('officer-assigned-regions-badge').textContent = `[${badges.join(', ')}]`;

    const reqTbody = document.querySelector('#table-officer-area-requests tbody');
    reqTbody.innerHTML = '';
    data.requests.forEach(r => {
      const tr = document.createElement('tr');
      const locLabel = regionNames[r.regionId] || ('LOC' + (r.regionId < 10 ? '0' + r.regionId : r.regionId));
      tr.innerHTML = `
        <td><code>${r.id}</code></td>
        <td><strong>${locLabel}</strong></td>
        <td><span class="badge-sev sev-badge-${r.severity}">${r.severity}</span></td>
        <td>${r.affectedPopulation.toLocaleString()}</td>
        <td><span class="badge-status status-${r.status.toLowerCase()}">${r.status}</span></td>
      `;
      reqTbody.appendChild(tr);
    });

    const allocTbody = document.querySelector('#table-officer-area-allocations tbody');
    allocTbody.innerHTML = '';
    data.incomingAllocations.forEach(a => {
      const tr = document.createElement('tr');
      const targetLabel = regionNames[a.targetRegionId] || ('LOC' + (a.targetRegionId < 10 ? '0' + a.targetRegionId : a.targetRegionId));
      tr.innerHTML = `
        <td><code>${a.id}</code></td>
        <td>${a.type}</td>
        <td>${targetLabel}</td>
        <td>${a.type === 'SUPPLY' ? `${a.medicineAllocated} med, ${a.foodAllocated} food` : `Team: ${a.teamId}`}</td>
        <td>${a.travelTime} min</td>
        <td><span class="badge-status status-${a.status.toLowerCase()}">${a.status}</span></td>
        <td>
          ${a.status === 'DISPATCHED' ? `<button class="btn btn-success btn-sm" onclick="deliverAllocation('${a.id}')">Confirm Receipt</button>` : ''}
        </td>
      `;
      allocTbody.appendChild(tr);
    });
  } catch (err) {}
}

// ---------------------------------------------------------------------------------------------
// SINGLE ASSESSMENT REPORT FORM & SEVERITY PREVIEW ENGINE (ADDITIONAL SPEC)
// ---------------------------------------------------------------------------------------------

let currentNeedOrder = ['RESCUE', 'SHELTER', 'MEDICINE'];
let previewDebounceTimer = null;

function populateRegionOptions() {
  const select = document.getElementById('report-region-select');
  const modalSelect = document.getElementById('modal-req-region');
  if (!select) return;

  const regions = [
    { id: 1, name: 'LOC01 - Aruvi Central (District Control)' },
    { id: 2, name: 'LOC02 - Northbank' },
    { id: 3, name: 'LOC03 - Hillview' },
    { id: 4, name: 'LOC04 - Riverside' },
    { id: 5, name: 'LOC05 - Greenfield' },
    { id: 6, name: 'LOC06 - Eastwood' },
    { id: 7, name: 'LOC07 - Lakeview' },
    { id: 8, name: 'LOC08 - Pinebridge' },
    { id: 9, name: 'LOC09 - Meadowpark' },
    { id: 10, name: 'LOC10 - Stonepass' }
  ];

  let allowedRegions = regions;
  if (currentUser && currentUser.role === 'FIELD_OFFICER' && currentUser.assignedLocationIds && currentUser.assignedLocationIds.length > 0) {
    allowedRegions = regions.filter(r => currentUser.assignedLocationIds.includes(r.id));
  }

  select.innerHTML = '';
  allowedRegions.forEach(r => {
    select.innerHTML += `<option value="${r.id}">${r.name}</option>`;
  });

  if (modalSelect) {
    modalSelect.innerHTML = '';
    regions.forEach(r => {
      modalSelect.innerHTML += `<option value="${r.id}">${r.name}</option>`;
    });
  }
}

function initReportFormView() {
  populateRegionOptions();
  syncNeedFirstList();
  triggerLiveSeverityPreview();
}

function toggleStartGuidance() {
  const box = document.getElementById('start-guidance-box');
  if (!box) return;
  const isHidden = (box.style.display === 'none');
  box.style.display = isHidden ? 'block' : 'none';
  const btn = document.getElementById('btn-toggle-guidance');
  if (btn) {
    btn.innerHTML = isHidden ? '✕ Hide Guidance' : 'ℹ️ START Guidance';
  }
}

function toggleNeedCard(type) {
  const chk = document.getElementById(`need-check-${type}`);
  if (chk) {
    chk.checked = !chk.checked;
    handleNeedCheckboxChanged(type.toUpperCase());
  }
}

function handleNeedCheckboxChanged(needType) {
  const typeLower = needType.toLowerCase();
  const chk = document.getElementById(`need-check-${typeLower}`);
  const isChecked = chk ? chk.checked : false;

  updateNeedCardVisual(typeLower, isChecked);

  if (isChecked) {
    if (!currentNeedOrder.includes(needType)) {
      currentNeedOrder.push(needType);
    }
  } else {
    currentNeedOrder = currentNeedOrder.filter(n => n !== needType);
  }

  // Validate at least one need
  const valMsg = document.getElementById('need-validation-error');
  if (valMsg) {
    valMsg.style.display = (currentNeedOrder.length === 0) ? 'block' : 'none';
  }

  syncNeedFirstList();
  triggerLiveSeverityPreview();
}

function updateNeedCardVisual(typeLower, isTicked) {
  const card = document.getElementById(`card-need-${typeLower}`);
  const body = document.getElementById(`body-need-${typeLower}`);
  if (card) {
    if (isTicked) card.classList.add('ticked');
    else card.classList.remove('ticked');
  }
  if (body) {
    body.style.display = isTicked ? 'block' : 'none';
  }
}

function syncNeedFirstList() {
  const container = document.getElementById('need-order-container');
  if (!container) return;
  container.innerHTML = '';

  if (currentNeedOrder.length === 0) {
    container.innerHTML = '<div style="color: #f87171; font-size: 13px; padding: 12px; background: rgba(239, 68, 68, 0.1); border-radius: 6px;">⚠️ No relief needs ticked. Please select at least one relief need above.</div>';
    document.getElementById('preview-first-need-val').textContent = 'None';
    return;
  }

  document.getElementById('preview-first-need-val').textContent = currentNeedOrder[0];

  currentNeedOrder.forEach((need, index) => {
    let icon = '📦';
    let label = need;
    let details = '';

    if (need === 'RESCUE') {
      icon = '🚒';
      label = 'Search & Rescue Extraction';
      const p = document.getElementById('need-rescue-people')?.value || '0';
      details = `${p} persons trapped`;
    } else if (need === 'SHELTER') {
      icon = '⛺';
      label = 'Emergency Evacuation Shelter';
      const p = document.getElementById('need-shelter-people')?.value || '0';
      details = `${p} displaced citizens`;
    } else if (need === 'MEDICINE') {
      icon = '💊';
      label = 'Medical Trauma Supplies';
      const k = document.getElementById('need-med-kits')?.value || '0';
      details = `${k} relief kits`;
    } else if (need === 'FOOD') {
      icon = '🍲';
      label = 'Food Rations & Water';
      const k = document.getElementById('need-food-kits')?.value || '0';
      details = `${k} ration packs`;
    }

    const itemEl = document.createElement('div');
    itemEl.className = `need-order-item ${index === 0 ? 'first-rank' : ''}`;
    itemEl.innerHTML = `
      <div style="display: flex; align-items: center; gap: 10px;">
        <span class="rank-badge">#${index + 1} ${index === 0 ? 'FIRST' : ''}</span>
        <div>
          <strong style="font-size: 14px;">${icon} ${label}</strong>
          <small style="color: var(--text-muted); display: block; font-size: 11px;">${details}</small>
        </div>
      </div>
      <div class="need-order-controls">
        <button type="button" class="order-btn" title="Move Up in Priority" onclick="moveNeedOrder(${index}, -1)" ${index === 0 ? 'disabled' : ''}>▲</button>
        <button type="button" class="order-btn" title="Move Down in Priority" onclick="moveNeedOrder(${index}, 1)" ${index === currentNeedOrder.length - 1 ? 'disabled' : ''}>▼</button>
      </div>
    `;
    container.appendChild(itemEl);
  });
}

function moveNeedOrder(index, direction) {
  const newIndex = index + direction;
  if (newIndex < 0 || newIndex >= currentNeedOrder.length) return;
  const temp = currentNeedOrder[index];
  currentNeedOrder[index] = currentNeedOrder[newIndex];
  currentNeedOrder[newIndex] = temp;
  syncNeedFirstList();
}

function updateMedicineKitCalculations() {
  const people = parseInt(document.getElementById('need-med-people')?.value) || 0;
  const suggested = Math.ceil(people / 10);
  const lbl = document.getElementById('med-suggested-label');
  if (lbl) lbl.textContent = `(Auto: ${suggested} kits)`;

  const reasonGroup = document.getElementById('group-med-reason');
  const kitsInput = document.getElementById('need-med-kits');
  // If reason is currently empty and hidden, auto-sync requested to suggested
  if (reasonGroup && reasonGroup.style.display === 'none' && kitsInput) {
    kitsInput.value = suggested;
  }
  checkMedicineKitOverride();
  triggerLiveSeverityPreview();
}

function checkMedicineKitOverride() {
  const people = parseInt(document.getElementById('need-med-people')?.value) || 0;
  const suggested = Math.ceil(people / 10);
  const req = parseInt(document.getElementById('need-med-kits')?.value) || 0;
  const reasonGroup = document.getElementById('group-med-reason');
  if (reasonGroup) {
    reasonGroup.style.display = (req !== suggested) ? 'block' : 'none';
  }
  syncNeedFirstList();
}

function updateFoodKitCalculations() {
  const people = parseInt(document.getElementById('need-food-people')?.value) || 0;
  const days = parseInt(document.getElementById('need-food-days')?.value) || 1;
  const suggested = Math.ceil((people * days) / 5);
  const lbl = document.getElementById('food-suggested-label');
  if (lbl) lbl.textContent = `(Auto: ${suggested} packs)`;

  const reasonGroup = document.getElementById('group-food-reason');
  const kitsInput = document.getElementById('need-food-kits');
  if (reasonGroup && reasonGroup.style.display === 'none' && kitsInput) {
    kitsInput.value = suggested;
  }
  checkFoodKitOverride();
  triggerLiveSeverityPreview();
}

function checkFoodKitOverride() {
  const people = parseInt(document.getElementById('need-food-people')?.value) || 0;
  const days = parseInt(document.getElementById('need-food-days')?.value) || 1;
  const suggested = Math.ceil((people * days) / 5);
  const req = parseInt(document.getElementById('need-food-kits')?.value) || 0;
  const reasonGroup = document.getElementById('group-food-reason');
  if (reasonGroup) {
    reasonGroup.style.display = (req !== suggested) ? 'block' : 'none';
  }
  syncNeedFirstList();
}

function triggerLiveSeverityPreview() {
  if (previewDebounceTimer) clearTimeout(previewDebounceTimer);
  previewDebounceTimer = setTimeout(async () => {
    const total = parseInt(document.getElementById('report-count-total')?.value) || 0;
    const red = parseInt(document.getElementById('report-count-red')?.value) || 0;
    const yellow = parseInt(document.getElementById('report-count-yellow')?.value) || 0;
    const green = parseInt(document.getElementById('report-count-green')?.value) || 0;
    const black = parseInt(document.getElementById('report-count-black')?.value) || 0;
    const trapped = parseInt(document.getElementById('report-count-trapped')?.value) || 0;
    const vulnerable = parseInt(document.getElementById('report-count-vulnerable')?.value) || 0;
    const hours = parseInt(document.getElementById('report-hours')?.value) || 0;
    const threat = document.getElementById('report-threat')?.checked || false;

    // Check category sum
    const sum = red + yellow + green + black;
    const valMsg = document.getElementById('count-validation-error');
    if (valMsg) {
      valMsg.style.display = (sum > total) ? 'block' : 'none';
    }

    const medPeople = parseInt(document.getElementById('need-med-people')?.value) || 0;
    const foodPeople = parseInt(document.getElementById('need-food-people')?.value) || 0;
    const foodDays = parseInt(document.getElementById('need-food-days')?.value) || 1;

    try {
      const res = await fetch('/api/severity/preview', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          total, red, yellow, green, black, trapped, vulnerable,
          hoursWithoutSupplies: hours,
          ongoingThreat: threat,
          medicinePeople: medPeople,
          foodPeople: foodPeople,
          foodDays: foodDays
        })
      });

      if (!res.ok) return;
      const data = await res.json();

      const badgeBox = document.getElementById('preview-badge-box');
      if (badgeBox) {
        badgeBox.className = `preview-badge-display sev-${data.level}`;
      }
      const lvlVal = document.getElementById('preview-level-val');
      if (lvlVal) lvlVal.textContent = data.level;
      const lblVal = document.getElementById('preview-label-val');
      if (lblVal) lblVal.textContent = data.label;
      const ruleVal = document.getElementById('preview-rule-val');
      if (ruleVal) ruleVal.textContent = data.ruleId;
      const expBox = document.getElementById('preview-explanation-box');
      if (expBox) expBox.textContent = data.explanation;

      const medVal = document.getElementById('preview-med-kits-val');
      if (medVal) medVal.textContent = `${data.suggestedMedicineKits} kits`;
      const foodVal = document.getElementById('preview-food-kits-val');
      if (foodVal) foodVal.textContent = `${data.suggestedFoodKits} packs`;
    } catch (err) {
      console.error('Severity preview calculation error:', err);
    }
  }, 180);
}

function resetReportForm() {
  document.getElementById('editing-report-id').value = '';
  document.getElementById('report-form-heading').textContent = 'Single Incident Assessment Report';
  document.getElementById('btn-submit-report').textContent = '📡 Transmit Assessment Report to Control Room';

  document.getElementById('report-road-access').value = 'OPEN';
  document.getElementById('report-count-total').value = '500';
  document.getElementById('report-count-red').value = '6';
  document.getElementById('report-count-yellow').value = '12';
  document.getElementById('report-count-green').value = '80';
  document.getElementById('report-count-black').value = '0';
  document.getElementById('report-count-trapped').value = '2';
  document.getElementById('report-count-vulnerable').value = '25';
  document.getElementById('report-hours').value = '14';
  document.getElementById('report-threat').checked = false;

  document.getElementById('need-check-rescue').checked = true;
  document.getElementById('need-rescue-people').value = '2';
  updateNeedCardVisual('rescue', true);

  document.getElementById('need-check-shelter').checked = true;
  document.getElementById('need-shelter-people').value = '45';
  updateNeedCardVisual('shelter', true);

  document.getElementById('need-check-medicine').checked = true;
  document.getElementById('need-med-people').value = '18';
  document.getElementById('need-med-kits').value = '2';
  document.getElementById('need-med-reason').value = '';
  updateNeedCardVisual('medicine', true);
  checkMedicineKitOverride();

  document.getElementById('need-check-food').checked = false;
  document.getElementById('need-food-people').value = '100';
  document.getElementById('need-food-days').value = '2';
  document.getElementById('need-food-kits').value = '40';
  document.getElementById('need-food-reason').value = '';
  updateNeedCardVisual('food', false);
  checkFoodKitOverride();

  currentNeedOrder = ['RESCUE', 'SHELTER', 'MEDICINE'];
  document.getElementById('report-notes').value = '';

  populateRegionOptions();
  syncNeedFirstList();
  triggerLiveSeverityPreview();
}

async function handleSingleReportSubmit(event) {
  event.preventDefault();

  if (currentNeedOrder.length === 0) {
    showToast('At least one relief need must be selected before submitting', 'warning');
    return;
  }

  const total = parseInt(document.getElementById('report-count-total').value) || 0;
  const red = parseInt(document.getElementById('report-count-red').value) || 0;
  const yellow = parseInt(document.getElementById('report-count-yellow').value) || 0;
  const green = parseInt(document.getElementById('report-count-green').value) || 0;
  const black = parseInt(document.getElementById('report-count-black').value) || 0;
  const trapped = parseInt(document.getElementById('report-count-trapped').value) || 0;
  const vulnerable = parseInt(document.getElementById('report-count-vulnerable').value) || 0;

  if (red + yellow + green + black > total) {
    showToast('Triage counts sum (Red + Yellow + Green + Black) cannot exceed Total Affected Population', 'danger');
    return;
  }

  const regionId = parseInt(document.getElementById('report-region-select').value);
  const roadAccessObserved = document.getElementById('report-road-access').value;
  const hoursWithoutSupplies = parseInt(document.getElementById('report-hours').value) || 0;
  const ongoingThreat = document.getElementById('report-threat').checked;
  const notes = document.getElementById('report-notes').value.trim();

  // Validate kit overrides
  const hasRescue = document.getElementById('need-check-rescue').checked;
  const hasShelter = document.getElementById('need-check-shelter').checked;
  const hasMedicine = document.getElementById('need-check-medicine').checked;
  const hasFood = document.getElementById('need-check-food').checked;

  let medNeed = null;
  if (hasMedicine) {
    const medPeople = parseInt(document.getElementById('need-med-people').value) || 0;
    const suggestedKits = Math.ceil(medPeople / 10);
    const requestedKits = parseInt(document.getElementById('need-med-kits').value) || 0;
    const medReason = document.getElementById('need-med-reason').value.trim();
    if (requestedKits !== suggestedKits && !medReason) {
      showToast('Justification reason required for custom medicine kit quantity', 'warning');
      document.getElementById('need-med-reason').focus();
      return;
    }
    medNeed = {
      people: medPeople,
      kitsSuggested: suggestedKits,
      kitsRequested: requestedKits,
      editReason: medReason
    };
  }

  let foodNeed = null;
  if (hasFood) {
    const foodPeople = parseInt(document.getElementById('need-food-people').value) || 0;
    const foodDays = parseInt(document.getElementById('need-food-days').value) || 1;
    const suggestedPacks = Math.ceil((foodPeople * foodDays) / 5);
    const requestedPacks = parseInt(document.getElementById('need-food-kits').value) || 0;
    const foodReason = document.getElementById('need-food-reason').value.trim();
    if (requestedPacks !== suggestedPacks && !foodReason) {
      showToast('Justification reason required for custom food packs quantity', 'warning');
      document.getElementById('need-food-reason').focus();
      return;
    }
    foodNeed = {
      people: foodPeople,
      days: foodDays,
      kitsSuggested: suggestedPacks,
      kitsRequested: requestedPacks,
      editReason: foodReason
    };
  }

  const payload = {
    regionId: regionId,
    roadAccessObserved: roadAccessObserved,
    counts: {
      total: total,
      red: red,
      yellow: yellow,
      green: green,
      black: black,
      trapped: trapped,
      vulnerable: vulnerable
    },
    hoursWithoutSupplies: hoursWithoutSupplies,
    ongoingThreat: ongoingThreat,
    needs: {
      rescue: hasRescue ? { people: parseInt(document.getElementById('need-rescue-people').value) || 0 } : null,
      shelter: hasShelter ? { people: parseInt(document.getElementById('need-shelter-people').value) || 0 } : null,
      medicine: medNeed,
      food: foodNeed
    },
    needOrder: currentNeedOrder,
    notes: notes
  };

  const editingId = document.getElementById('editing-report-id').value;

  try {
    if (editingId) {
      const updated = await fetchApi(`/api/reports/${editingId}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      });
      showToast(`Report ${editingId} updated successfully (v${updated.version})! Severity re-evaluated.`, 'success');
    } else {
      const created = await fetchApi('/api/reports', {
        method: 'POST',
        body: JSON.stringify(payload)
      });
      showToast(`Incident Assessment transmitted! Report ID: ${created.id} (Severity ${created.severity?.effectiveLevel || 3})`, 'success');
    }

    resetReportForm();

    if (currentUser?.role === 'FIELD_OFFICER') {
      switchView('officer-reports');
    } else {
      switchView('inbox');
      refreshAllData();
    }
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

// Alias for backwards compatibility
const handleOfficerSubmitReport = handleSingleReportSubmit;

async function loadOfficerReports() {
  try {
    const reports = await fetchApi('/api/reports/mine');
    const tbody = document.querySelector('#table-officer-reports tbody');
    tbody.innerHTML = '';

    if (!reports || reports.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: var(--text-dim); padding: 24px;">You have not submitted any incident assessments yet.</td></tr>';
      return;
    }

    reports.forEach(r => {
      const tr = document.createElement('tr');
      const sev = r.severity || {};
      const effectiveLevel = (sev.overrideLevel != null) ? sev.overrideLevel : (sev.calculated || 3);
      const isOverridden = (sev.overrideLevel != null);

      let statusBadge = `<span class="badge-status status-waiting">${r.status}</span>`;
      if (r.status === 'APPROVED') statusBadge = `<span class="badge-status status-allocated">APPROVED</span>`;
      if (r.status === 'REJECTED') statusBadge = `<span class="badge-status status-blocked">REJECTED</span>`;

      let auditReason = '-';
      if (r.status === 'REJECTED' && r.rejectionReason) {
        auditReason = `<span style="color: #f87171;">Rejected: ${r.rejectionReason}</span>`;
      } else if (isOverridden) {
        auditReason = `<span style="color: #fbbf24;">Overridden to Level ${sev.overrideLevel}: ${sev.overrideReason}</span>`;
      } else if (r.version > 1) {
        auditReason = `<span style="color: #38bdf8;">Recount Updated (v${r.version})</span>`;
      }

      const needsOrderStr = (r.needOrder || []).join(' &rarr; ') || '-';

      tr.innerHTML = `
        <td><strong style="color: #60a5fa; font-family: var(--font-mono);">${r.id}</strong></td>
        <td>Region ${r.regionId}</td>
        <td><span class="rank-badge" style="margin-right: 0;">v${r.version}</span></td>
        <td>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span class="badge-sev sev-badge-${effectiveLevel}">${effectiveLevel}</span>
            <small style="font-size: 11px;">${isOverridden ? '⚠️ Overridden' : 'Calculated'}</small>
          </div>
        </td>
        <td style="font-size: 12px; font-family: var(--font-mono);">${needsOrderStr}</td>
        <td>${statusBadge}</td>
        <td style="font-size: 12px; max-width: 220px;">${auditReason}</td>
        <td>
          <div style="display: flex; gap: 6px;">
            <button class="btn btn-secondary btn-sm" onclick="loadReportIntoForm('${r.id}')" title="Edit or recount assessment">✏️ Edit</button>
            <button class="btn btn-secondary btn-sm" onclick="viewReportDetails('${r.id}')" title="View details">👁️ Details</button>
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error('loadOfficerReports error:', err);
  }
}

async function loadReportIntoForm(reportId) {
  try {
    const r = await fetchApi('/api/reports/' + reportId);
    if (!r) return;

    switchView('officer-new');

    document.getElementById('editing-report-id').value = r.id;
    document.getElementById('report-form-heading').textContent = `Update Assessment Report (${r.id} v${r.version})`;
    document.getElementById('btn-submit-report').textContent = '💾 Transmit Recount / Update to Control Room';

    // Section A
    populateRegionOptions();
    document.getElementById('report-region-select').value = r.regionId;
    document.getElementById('report-road-access').value = r.roadAccessObserved || 'OPEN';

    // Section B
    const c = r.counts || {};
    document.getElementById('report-count-total').value = c.total || 0;
    document.getElementById('report-count-red').value = c.red || 0;
    document.getElementById('report-count-yellow').value = c.yellow || 0;
    document.getElementById('report-count-green').value = c.green || 0;
    document.getElementById('report-count-black').value = c.black || 0;
    document.getElementById('report-count-trapped').value = c.trapped || 0;
    document.getElementById('report-count-vulnerable').value = c.vulnerable || 0;

    // Section C
    document.getElementById('report-hours').value = r.hoursWithoutSupplies || 0;
    document.getElementById('report-threat').checked = !!r.ongoingThreat;

    // Section D
    const n = r.needs || {};
    const hasRescue = !!(n.rescue && n.rescue.people > 0);
    document.getElementById('need-check-rescue').checked = hasRescue;
    document.getElementById('need-rescue-people').value = n.rescue ? n.rescue.people : 0;
    updateNeedCardVisual('rescue', hasRescue);

    const hasShelter = !!(n.shelter && n.shelter.people > 0);
    document.getElementById('need-check-shelter').checked = hasShelter;
    document.getElementById('need-shelter-people').value = n.shelter ? n.shelter.people : 0;
    updateNeedCardVisual('shelter', hasShelter);

    const hasMed = !!(n.medicine && (n.medicine.people > 0 || n.medicine.kitsRequested > 0));
    document.getElementById('need-check-medicine').checked = hasMed;
    document.getElementById('need-med-people').value = n.medicine ? n.medicine.people : 0;
    document.getElementById('need-med-kits').value = n.medicine ? n.medicine.kitsRequested : 0;
    document.getElementById('need-med-reason').value = n.medicine ? (n.medicine.editReason || '') : '';
    updateNeedCardVisual('medicine', hasMed);
    checkMedicineKitOverride();

    const hasFood = !!(n.food && (n.food.people > 0 || n.food.kitsRequested > 0));
    document.getElementById('need-check-food').checked = hasFood;
    document.getElementById('need-food-people').value = n.food ? n.food.people : 0;
    document.getElementById('need-food-days').value = n.food ? (n.food.days || 1) : 1;
    document.getElementById('need-food-kits').value = n.food ? n.food.kitsRequested : 0;
    document.getElementById('need-food-reason').value = n.food ? (n.food.editReason || '') : '';
    updateNeedCardVisual('food', hasFood);
    checkFoodKitOverride();

    // Section E
    currentNeedOrder = (r.needOrder && r.needOrder.length > 0) ? [...r.needOrder] : ['RESCUE'];
    syncNeedFirstList();

    // Section F
    document.getElementById('report-notes').value = r.notes || '';

    triggerLiveSeverityPreview();
    showToast(`Loaded ${r.id} for editing/recount`, 'info');
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function viewReportDetails(reportId) {
  try {
    const r = await fetchApi('/api/reports/' + reportId);
    if (!r) return;

    document.getElementById('view-report-title').textContent = `Incident Assessment Details: ${r.id} (v${r.version})`;
    const body = document.getElementById('view-report-body');

    const c = r.counts || {};
    const sev = r.severity || {};
    const effLevel = (sev.overrideLevel != null) ? sev.overrideLevel : (sev.calculated || 1);
    const n = r.needs || {};

    let needsHtml = '';
    if (n.rescue) needsHtml += `<div style="margin-bottom: 4px;">🚒 <strong>Rescue:</strong> ${n.rescue.people} people to extract</div>`;
    if (n.shelter) needsHtml += `<div style="margin-bottom: 4px;">⛺ <strong>Shelter:</strong> ${n.shelter.people} people to shelter</div>`;
    if (n.medicine) needsHtml += `<div style="margin-bottom: 4px;">💊 <strong>Medicine:</strong> ${n.medicine.people} injured, ${n.medicine.kitsRequested} kits requested (suggested: ${n.medicine.kitsSuggested})${n.medicine.editReason ? ` &bull; <em>Reason: ${n.medicine.editReason}</em>` : ''}</div>`;
    if (n.food) needsHtml += `<div style="margin-bottom: 4px;">🍲 <strong>Food:</strong> ${n.food.people} people for ${n.food.days} days, ${n.food.kitsRequested} packs requested (suggested: ${n.food.kitsSuggested})${n.food.editReason ? ` &bull; <em>Reason: ${n.food.editReason}</em>` : ''}</div>`;
    if (!needsHtml) needsHtml = '<div>None recorded</div>';

    let historyHtml = '';
    if (r.history && r.history.length > 0) {
      historyHtml = r.history.map(h => `
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); padding: 8px 12px; border-radius: 6px; margin-top: 6px; font-size: 12px;">
          <div style="display: flex; justify-content: space-between;">
            <strong>${h.action}</strong>
            <small style="color: var(--text-muted);">${new Date(h.timestamp).toLocaleTimeString()}</small>
          </div>
          <div style="color: var(--text-muted); margin-top: 2px;">User: <code>${h.user}</code> &bull; Version: ${h.version}</div>
          <div style="color: var(--text-main); margin-top: 4px;">${h.summary}</div>
        </div>
      `).join('');
    } else {
      historyHtml = '<div style="color: var(--text-muted); font-size: 12px;">Initial submission version 1</div>';
    }

    let linkedHtml = '';
    if (r.linkedRequestIds && r.linkedRequestIds.length > 0) {
      linkedHtml = r.linkedRequestIds.map(id => `<span style="font-family: var(--font-mono); background: rgba(56, 189, 248, 0.15); color: #38bdf8; padding: 2px 8px; border-radius: 4px; margin-right: 6px;">${id}</span>`).join('');
    } else {
      linkedHtml = '<span style="color: var(--text-muted);">None generated yet (Pending Review)</span>';
    }

    body.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
        <div style="background: var(--bg-card-alt); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
          <div style="color: var(--text-muted); font-size: 11px; text-transform: uppercase;">Location &amp; Reporter</div>
          <div style="font-size: 15px; font-weight: 700; margin-top: 4px;">Region ${r.regionId}</div>
          <div style="font-size: 12px; color: var(--text-dim); margin-top: 2px;">Officer: <code>${r.officerId}</code> &bull; Status: <strong>${r.status}</strong></div>
          <div style="font-size: 12px; color: var(--text-dim); margin-top: 2px;">Road Access: <strong>${r.roadAccessObserved || 'OPEN'}</strong></div>
        </div>
        <div style="background: var(--bg-card-alt); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
          <div style="color: var(--text-muted); font-size: 11px; text-transform: uppercase;">Severity Classification</div>
          <div style="display: flex; align-items: center; gap: 10px; margin-top: 4px;">
            <span class="badge-sev sev-badge-${effLevel}" style="width: 32px; height: 32px; font-size: 16px;">${effLevel}</span>
            <div>
              <strong style="font-size: 14px;">${getSeverityLabel(effLevel)}</strong>
              ${sev.overrideLevel != null ? '<span style="color: #fbbf24; font-size: 11px; display: block;">⚠️ Control Room Override</span>' : ''}
            </div>
          </div>
          <div style="font-size: 11px; color: var(--text-muted); margin-top: 4px;">Rule: <code>${sev.ruleId || 'N/A'}</code></div>
        </div>
      </div>

      <div style="background: var(--bg-card-alt); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); margin-bottom: 16px;">
        <div style="color: var(--text-muted); font-size: 11px; text-transform: uppercase; margin-bottom: 6px;">START Mass-Casualty Counts</div>
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; text-align: center; font-size: 12px;">
          <div style="background: rgba(239, 68, 68, 0.15); padding: 6px; border-radius: 4px; border: 1px solid rgba(239,68,68,0.3);"><strong style="color: #f87171;">Red (Immediate)</strong><div style="font-size: 18px; font-weight: 800;">${c.red || 0}</div></div>
          <div style="background: rgba(234, 179, 8, 0.15); padding: 6px; border-radius: 4px; border: 1px solid rgba(234,179,8,0.3);"><strong style="color: #fbbf24;">Yellow (Delayed)</strong><div style="font-size: 18px; font-weight: 800;">${c.yellow || 0}</div></div>
          <div style="background: rgba(16, 185, 129, 0.15); padding: 6px; border-radius: 4px; border: 1px solid rgba(16,185,129,0.3);"><strong style="color: #34d399;">Green (Minor)</strong><div style="font-size: 18px; font-weight: 800;">${c.green || 0}</div></div>
          <div style="background: rgba(100, 116, 139, 0.2); padding: 6px; border-radius: 4px; border: 1px solid rgba(100,116,139,0.4);"><strong style="color: #cbd5e1;">Black (Deceased)</strong><div style="font-size: 18px; font-weight: 800;">${c.black || 0}</div></div>
        </div>
        <div style="margin-top: 10px; font-size: 12px; color: var(--text-muted); display: flex; justify-content: space-between;">
          <span>Trapped Under Debris: <strong>${c.trapped || 0}</strong></span>
          <span>Vulnerable Citizens: <strong>${c.vulnerable || 0}</strong></span>
          <span>Total Population: <strong>${(c.total || 0).toLocaleString()}</strong></span>
        </div>
      </div>

      <div style="background: var(--bg-card-alt); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); margin-bottom: 16px;">
        <div style="color: var(--text-muted); font-size: 11px; text-transform: uppercase; margin-bottom: 6px;">Relief Needs &amp; Order</div>
        <div style="margin-bottom: 8px;"><strong>Need-First Queue Order:</strong> ${(r.needOrder || []).map((no, idx) => `<span class="rank-badge">#${idx+1} ${no}</span>`).join(' ')}</div>
        ${needsHtml}
      </div>

      <div style="background: var(--bg-card-alt); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-color); margin-bottom: 16px;">
        <div style="color: var(--text-muted); font-size: 11px; text-transform: uppercase; margin-bottom: 6px;">Linked Triage Queue Requests</div>
        <div>${linkedHtml}</div>
      </div>

      <div style="background: var(--bg-card-alt); padding: 12px; border-radius: var(--radius-sm); border: 1px solid var(--border-color);">
        <div style="color: var(--text-muted); font-size: 11px; text-transform: uppercase; margin-bottom: 6px;">Audit Trail &amp; Version History</div>
        ${historyHtml}
      </div>
    `;

    openModal('modal-view-report');
  } catch (err) {
    showToast(err.message, 'danger');
  }
}

async function handleOfficerSubmitRoadReport(event) {
  event.preventDefault();
  const roadId = document.getElementById('hazard-road').value;
  const hazard = document.getElementById('hazard-details').value.trim();

  try {
    await fetchApi('/api/officer/road-report', {
      method: 'POST',
      body: JSON.stringify({ roadId, hazard })
    });
    showToast('Road hazard warning transmitted to Control Room!', 'warning');
    document.getElementById('hazard-details').value = '';
    switchView('officer-area');
  } catch (err) { showToast(err.message, 'danger'); }
}

// ---------------------------------------------------------------------------------------------
// MODALS & COMMON UTILS
// ---------------------------------------------------------------------------------------------

function openModal(modalId) {
  document.getElementById(modalId).classList.add('active');
}

function closeModal(modalId) {
  document.getElementById(modalId).classList.remove('active');
}

function openAddRequestModal() {
  openModal('modal-add-request');
}

async function handleAddRequestSubmit(event) {
  event.preventDefault();
  const regionId = parseInt(document.getElementById('modal-req-region').value);
  const severity = parseInt(document.getElementById('modal-req-severity').value);
  const affectedPopulation = parseInt(document.getElementById('modal-req-pop').value);
  const mostUrgentNeed = document.getElementById('modal-req-urgent').value;
  const peopleToEvacuate = parseInt(document.getElementById('modal-req-evac').value);
  const medicineNeeded = parseInt(document.getElementById('modal-req-med').value);
  const foodNeeded = parseInt(document.getElementById('modal-req-food').value);

  try {
    await fetchApi('/api/requests', {
      method: 'POST',
      body: JSON.stringify({
        regionId, severity, affectedPopulation, mostUrgentNeed,
        peopleToEvacuate, medicineNeeded, foodNeeded
      })
    });
    showToast('Emergency request added and enqueued!', 'success');
    closeModal('modal-add-request');
    refreshAllData();
  } catch (err) { showToast(err.message, 'danger'); }
}

async function triggerResetDemoData() {
  if (!confirm('Reset entire system dataset to initial demonstration state?')) return;
  try {
    await fetchApi('/api/admin/reset', { method: 'POST' });
    showToast('System database reset to factory seed data!', 'success');
    refreshAllData();
  } catch (err) { showToast(err.message, 'danger'); }
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : type === 'danger' ? '✗' : 'ℹ'}</span> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}
