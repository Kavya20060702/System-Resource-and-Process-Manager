// ═══════════════════════════════════════════════
//  SmartOS Monitor — Frontend App
//  Connects to Spring Boot backend at :8080
// ═══════════════════════════════════════════════

const API = 'http://localhost:8082/api';

// Process colors for Gantt chart
const PROCESS_COLORS = [
  { bg: '#00ff88', text: '#001a0e' },
  { bg: '#00ccff', text: '#001520' },
  { bg: '#ffaa00', text: '#1a0e00' },
  { bg: '#ff3355', text: '#1a0005' },
  { bg: '#aa55ff', text: '#0e0015' },
  { bg: '#ff6600', text: '#1a0900' },
  { bg: '#55ffaa', text: '#001a0a' },
  { bg: '#ff55cc', text: '#1a000e' },
];

// Chart history buffers
const MAX_POINTS = 30;
let cpuHistory = [];
let memHistory = [];
let timeLabels = [];

// ─────────────────────────────────────────
// CLOCK
// ─────────────────────────────────────────
function updateClock() {
  document.getElementById('clock').textContent =
    new Date().toLocaleTimeString('en-IN', { hour12: false });
}
setInterval(updateClock, 1000);
updateClock();

// ─────────────────────────────────────────
// TAB NAVIGATION
// ─────────────────────────────────────────
function showTab(name) {
  document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.getElementById(`tab-${name}`).classList.add('active');
  event.target.classList.add('active');

  // Load tab-specific data
  if (name === 'processes') loadProcesses();
  if (name === 'network') loadNetwork();
  if (name === 'history') loadHistory();
}

// ─────────────────────────────────────────
// CHART SETUP
// ─────────────────────────────────────────
const chartDefaults = {
  responsive: true,
  maintainAspectRatio: true,
  animation: { duration: 300 },
  plugins: { legend: { display: false } },
  scales: {
    x: {
      ticks: { color: '#4a6070', font: { family: 'Share Tech Mono', size: 9 } },
      grid: { color: '#1e2d3a' }
    },
    y: {
      min: 0, max: 100,
      ticks: { color: '#4a6070', font: { family: 'Share Tech Mono', size: 9 },
               callback: v => v + '%' },
      grid: { color: '#1e2d3a' }
    }
  }
};

const cpuChart = new Chart(document.getElementById('cpuChart'), {
  type: 'line',
  data: {
    labels: [],
    datasets: [{
      data: [],
      borderColor: '#00ff88',
      backgroundColor: 'rgba(0,255,136,0.06)',
      borderWidth: 1.5,
      fill: true,
      tension: 0.4,
      pointRadius: 0,
    }]
  },
  options: { ...chartDefaults }
});

const memChart = new Chart(document.getElementById('memChart'), {
  type: 'line',
  data: {
    labels: [],
    datasets: [{
      data: [],
      borderColor: '#00ccff',
      backgroundColor: 'rgba(0,204,255,0.06)',
      borderWidth: 1.5,
      fill: true,
      tension: 0.4,
      pointRadius: 0,
    }]
  },
  options: { ...chartDefaults }
});

// ─────────────────────────────────────────
// MAIN DASHBOARD REFRESH (every 3 seconds)
// ─────────────────────────────────────────
async function refreshDashboard() {
  try {
    const [cpuRes, memRes, diskRes, netRes] = await Promise.all([
      fetch(`${API}/cpu`),
      fetch(`${API}/memory`),
      fetch(`${API}/disk`),
      fetch(`${API}/network`)
    ]);

    const cpu  = await cpuRes.json();
    const mem  = await memRes.json();
    const disk = await diskRes.json();
    const net  = await netRes.json();

    updateCpuCard(cpu);
    updateMemCard(mem);
    updateDiskCard(disk);
    updateNetCard(net);
    updateCharts(cpu.usagePercent, mem.usagePercent);
    updateCores(cpu.perCoreUsage);
    loadAlerts(); 
  } catch (err) {
    console.warn('Error fetching dashboard:', err);
    showMockData();
  }
}
async function loadAlerts() {
  try {
    const res = await fetch(`${API}/alerts`);
    const alerts = await res.json();

    // Only show unresolved alerts
    const active = alerts.filter(a => a.resolved === false);
    const list = document.getElementById('alerts-list');

    if (!active.length) {
      list.innerHTML = '<div style="color:var(--text-dim)">No active alerts</div>';
      return;
    }

    list.innerHTML = active.map(a => `
      <div class="alert-item ${a.severity === 'CRITICAL' ? 'critical' : ''}">
        <strong>${a.alertType}</strong> — ${a.message}
        <span style="color:var(--text-dim); font-size:11px; margin-left:10px">
          ${a.createdAt?.replace('T',' ').substring(0,19)}
        </span>
        <button onclick="resolveAlert(${a.id})" 
          style="float:right; background:transparent; border:1px solid var(--border); 
                 color:var(--text-dim); font-size:10px; padding:2px 8px; cursor:pointer;">
          RESOLVE
        </button>
      </div>
    `).join('');
  } catch(e) {
    console.warn('Alerts unavailable');
  }
}

async function resolveAlert(id) {
  try {
    const res = await fetch(`${API}/alerts/${id}/resolve`, { method: 'PUT' });
    const result = await res.json();

    // Show recommendation popup
    const list = document.getElementById('alerts-list');
    const div = document.createElement('div');
    div.style.cssText = `
      padding: 12px; margin: 8px 0;
      background: rgba(0,255,136,0.05);
      border-left: 3px solid var(--accent);
      font-size: 12px;
    `;

    // Build offenders list
    let offendersHtml = '';
    if (result.topOffenders && result.topOffenders.length) {
      offendersHtml = `
        <div style="margin-top:8px; color:var(--text-dim)">
          Top offenders:
          ${result.topOffenders.map(p => `
            <span style="
              margin-left:8px; padding:2px 8px;
              border:1px solid var(--border);
              color:var(--warn)">
              ${p.name} — ${p.cpu !== undefined ? p.cpu + '% CPU' : p.memoryMB + ' MB'}
            </span>
          `).join('')}
        </div>
      `;
    }

    div.innerHTML = `
      <span style="color:var(--accent)">✅ RESOLVED</span>
      <span style="margin-left:10px; color:var(--text-dim)">
        ${result.recommendation || 'Alert acknowledged'}
      </span>
      ${offendersHtml}
    `;

    list.prepend(div);

    // Remove after 8 seconds
    setTimeout(() => div.remove(), 8000);

    // Refresh alerts list
    loadAlerts();

  } catch(e) {
    console.warn('Resolve failed:', e);
  }
}

// ─────────────────────────────────────────
// CARD UPDATES
// ─────────────────────────────────────────
function updateCpuCard(cpu) {
  const pct = cpu.usagePercent;
  document.getElementById('cpu-val').textContent = pct + '%';
  document.getElementById('cpu-cores').textContent =
    `${cpu.physicalCores} cores · ${cpu.logicalCores} logical`;
  setBar('cpu-bar', pct);
  document.getElementById('hostname').textContent = cpu.processorName?.substring(0, 30) || 'N/A';
}

function updateMemCard(mem) {
  const pct = mem.usagePercent;
  document.getElementById('mem-val').textContent = pct + '%';
  document.getElementById('mem-detail').textContent =
    `${mem.usedGB} / ${mem.totalGB} GB`;
  setBar('mem-bar', pct);
}

function updateDiskCard(disks) {
  if (!disks || !disks.length) return;
  const d = disks[0];
  document.getElementById('disk-val').textContent = d.usagePercent + '%';
  document.getElementById('disk-detail').textContent =
    `${Math.round(d.usedBytes / 1073741824)} / ${d.totalGB} GB`;
  setBar('disk-bar', d.usagePercent);
}

function updateNetCard(net) {
  document.getElementById('net-conn').textContent = net.activeConnections + ' conn';
  document.getElementById('net-detail').textContent =
    `↑ ${formatBytes(net.totalBytesSent)} ↓ ${formatBytes(net.totalBytesRecv)}`;
  const usage = Math.min((net.activeConnections / 200) * 100, 100);
  setBar('net-bar', usage);
  document.getElementById('local-ip').textContent = net.localIp || '---';
}

function setBar(id, pct) {
  const bar = document.getElementById(id);
  bar.style.width = pct + '%';
  bar.classList.remove('warn', 'danger');
  if (pct > 90) bar.classList.add('danger');
  else if (pct > 70) bar.classList.add('warn');
}

// ─────────────────────────────────────────
// LIVE CHARTS
// ─────────────────────────────────────────
function updateCharts(cpuPct, memPct) {
  const label = new Date().toLocaleTimeString('en-IN', { hour12: false });
  timeLabels.push(label);
  cpuHistory.push(cpuPct);
  memHistory.push(memPct);

  if (timeLabels.length > MAX_POINTS) {
    timeLabels.shift(); cpuHistory.shift(); memHistory.shift();
  }

  cpuChart.data.labels = [...timeLabels];
  cpuChart.data.datasets[0].data = [...cpuHistory];
  cpuChart.update('none');

  memChart.data.labels = [...timeLabels];
  memChart.data.datasets[0].data = [...memHistory];
  memChart.update('none');
}

// ─────────────────────────────────────────
// PER-CORE GRID
// ─────────────────────────────────────────
function updateCores(cores) {
  if (!cores || !cores.length) return;
  const grid = document.getElementById('cores-grid');
  grid.innerHTML = cores.map((pct, i) => `
    <div class="core-box">
      <div class="core-label">CORE ${i}</div>
      <div class="core-val">${pct}%</div>
      <div class="core-bar-wrap">
        <div class="core-bar" style="width:${pct}%;
          background:${pct > 80 ? 'var(--danger)' : pct > 60 ? 'var(--warn)' : 'var(--accent2)'}">
        </div>
      </div>
    </div>
  `).join('');
}

// ─────────────────────────────────────────
// PROCESS TAB
// ─────────────────────────────────────────
async function loadProcesses() {
  try {
    const res = await fetch(`${API}/processes?limit=25`);
    const procs = await res.json();
    const tbody = document.getElementById('process-tbody');
    tbody.innerHTML = procs.map(p => {
      const rowClass = p.cpuPercent > 50 ? 'critical' : p.cpuPercent > 20 ? 'high-cpu' : '';
      return `<tr class="${rowClass}">
        <td>${p.pid}</td>
        <td>${p.name}</td>
        <td>${p.cpuPercent}%</td>
        <td>${p.memoryMB} MB</td>
        <td>${p.threads}</td>
        <td><span style="color:${stateColor(p.state)}">${p.state}</span></td>
        <td>${p.user || 'system'}</td>
        <td>${p.priority}</td>
      </tr>`;
    }).join('');
  } catch (e) {
    document.getElementById('process-tbody').innerHTML =
      '<tr><td colspan="8" style="color:var(--text-dim)">Backend not running — start Spring Boot first</td></tr>';
  }
}

function stateColor(state) {
  if (!state) return 'var(--text-dim)';
  const s = state.toUpperCase();
  if (s.includes('RUN')) return 'var(--accent)';
  if (s.includes('SLEEP') || s.includes('WAIT')) return 'var(--accent2)';
  if (s.includes('STOP')) return 'var(--warn)';
  if (s.includes('ZOMBIE')) return 'var(--danger)';
  return 'var(--text-dim)';
}

// ─────────────────────────────────────────
// NETWORK TAB
// ─────────────────────────────────────────
async function loadNetwork() {
  try {
    const res = await fetch(`${API}/network`);
    const net = await res.json();

    document.getElementById('net-hostname').textContent = net.hostname || '---';
    document.getElementById('net-ip').textContent = net.localIp || '---';
    document.getElementById('net-active').textContent = net.activeConnections;
    document.getElementById('net-total').textContent = net.totalConnections;

    const tbody = document.getElementById('net-interfaces');
    tbody.innerHTML = (net.interfaces || []).map(i => `
      <tr>
        <td>${i.name} — ${i.displayName}</td>
        <td>${i.ipv4}</td>
        <td>${i.macAddress}</td>
        <td>${formatBytes(i.bytesSent)}</td>
        <td>${formatBytes(i.bytesRecv)}</td>
      </tr>
    `).join('');

    loadPorts();
  } catch (e) {
    console.warn('Network data unavailable');
  }
}

async function loadPorts() {
  try {
    const res = await fetch(`${API}/ports`);
    const ports = await res.json();
    const tbody = document.getElementById('ports-tbody');
    tbody.innerHTML = ports.slice(0, 30).map(p => `
      <tr>
        <td style="color:var(--accent)">${p.localPort}</td>
        <td>${p.remoteAddress}</td>
        <td>${p.remotePort}</td>
        <td style="color:${p.state === 'ESTABLISHED' ? 'var(--accent)' : 'var(--text-dim)'}">
          ${p.state}
        </td>
        <td>${p.protocol}</td>
        <td>${p.pid}</td>
      </tr>
    `).join('');
  } catch (e) {
    document.getElementById('ports-tbody').innerHTML =
      '<tr><td colspan="6" style="color:var(--text-dim)">Data unavailable</td></tr>';
  }
}

// ─────────────────────────────────────────
// CPU SCHEDULER TAB
// ─────────────────────────────────────────
document.getElementById('algo-select').addEventListener('change', function() {
  document.getElementById('quantum-group').style.display =
    this.value === 'rr' ? 'flex' : 'none';
});

function addProcessRow() {
  const tbody = document.getElementById('input-tbody');
  const rowCount = tbody.rows.length + 1;
  const row = document.createElement('tr');
  row.innerHTML = `
    <td><input class="cell-input" value="P${rowCount}"/></td>
    <td><input class="cell-input" type="number" value="0"/></td>
    <td><input class="cell-input" type="number" value="2"/></td>
    <td><button class="btn-del" onclick="removeRow(this)">✕</button></td>
  `;
  tbody.appendChild(row);
}

function removeRow(btn) {
  btn.closest('tr').remove();
}

async function runSimulation() {
  const algo = document.getElementById('algo-select').value;
  const quantum = parseInt(document.getElementById('quantum-input').value) || 2;

  const rows = document.getElementById('input-tbody').rows;
  const processes = Array.from(rows).map(row => {
    const inputs = row.querySelectorAll('input');
    return {
      id: inputs[0].value.trim() || 'P?',
      arrivalTime: parseInt(inputs[1].value) || 0,
      burstTime: Math.max(1, parseInt(inputs[2].value) || 1),
      priority: 0
    };
  });

  if (!processes.length) return;

  try {
    const res = await fetch(`${API}/scheduler/simulate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ algorithm: algo, quantum, processes })
    });
    const result = await res.json();
    renderSchedulerResult(result, processes);
  } catch (err) {
    // Fallback: run FCFS in browser for demo
    const result = simulateFCFS_browser(processes);
    renderSchedulerResult(result, processes);
  }
}

// Client-side FCFS fallback (so Gantt works even without backend)
function simulateFCFS_browser(processes) {
  const sorted = [...processes].sort((a, b) => a.arrivalTime - b.arrivalTime);
  let time = 0;
  const gantt = [];
  const stats = [];

  for (const p of sorted) {
    if (time < p.arrivalTime) time = p.arrivalTime;
    const wait = time - p.arrivalTime;
    const finish = time + p.burstTime;
    gantt.push({ processId: p.id, start: time, end: finish });
    stats.push({ id: p.id, arrivalTime: p.arrivalTime, burstTime: p.burstTime,
                 waitingTime: wait, turnaround: finish - p.arrivalTime, finishTime: finish });
    time = finish;
  }

  const avgWT = stats.reduce((s, p) => s + p.waitingTime, 0) / stats.length;
  const avgTT = stats.reduce((s, p) => s + p.turnaround, 0) / stats.length;
  const totalBurst = processes.reduce((s, p) => s + p.burstTime, 0);
  return { ganttChart: gantt, processStats: stats, avgWaitingTime: avgWT,
           avgTurnaroundTime: avgTT, cpuUtilization: Math.round(totalBurst / time * 1000) / 10,
           algorithm: 'FCFS (Browser)' };
}

function renderSchedulerResult(result, processes) {
  document.getElementById('gantt-section').style.display = 'block';

  // Build process→color map
  const colorMap = {};
  const ids = [...new Set(result.ganttChart.map(b => b.processId))];
  ids.forEach((id, i) => { colorMap[id] = PROCESS_COLORS[i % PROCESS_COLORS.length]; });

  // Gantt chart
  const ganttEl = document.getElementById('gantt-chart');
  ganttEl.innerHTML = result.ganttChart.map(block => {
    const col = colorMap[block.processId];
    const width = Math.max(40, (block.end - block.start) * 40);
    return `
      <div class="gantt-block" title="${block.processId}: t${block.start}→t${block.end}"
           style="width:${width}px; background:${col.bg}; color:${col.text};">
        <span class="gp-id">${block.processId}</span>
        <span class="gp-time">${block.start}→${block.end}</span>
      </div>
    `;
  }).join('');

  // Stats table
  const tbody = document.getElementById('sched-stats-tbody');
  tbody.innerHTML = result.processStats.map(p => `
    <tr>
      <td style="color:${colorMap[p.id]?.bg || 'var(--accent)'}">${p.id}</td>
      <td>${p.arrivalTime}</td>
      <td>${p.burstTime}</td>
      <td>${p.finishTime}</td>
      <td style="color:var(--warn)">${p.waitingTime}</td>
      <td style="color:var(--accent2)">${p.turnaround}</td>
    </tr>
  `).join('');

  // Summary
  document.getElementById('sched-summary').innerHTML = `
    <div class="sched-stat">
      <label>ALGORITHM</label>
      <span style="font-size:16px">${result.algorithm}</span>
    </div>
    <div class="sched-stat">
      <label>AVG WAITING TIME</label>
      <span>${result.avgWaitingTime.toFixed(2)}</span>
    </div>
    <div class="sched-stat">
      <label>AVG TURNAROUND</label>
      <span>${result.avgTurnaroundTime.toFixed(2)}</span>
    </div>
    <div class="sched-stat">
      <label>CPU UTILIZATION</label>
      <span>${result.cpuUtilization}%</span>
    </div>
  `;
}

// ─────────────────────────────────────────
// HISTORY TAB (DBMS)
// ─────────────────────────────────────────
async function loadHistory() {
  try {
    const res = await fetch(`${API}/history/stats`);
    const data = await res.json();

    document.getElementById('hist-cpu24').textContent =
      data.avgCpuLast24h ? Math.round(data.avgCpuLast24h * 10) / 10 + '%' : 'N/A';
    document.getElementById('hist-cpu7d').textContent =
      data.avgCpuLast7d ? Math.round(data.avgCpuLast7d * 10) / 10 + '%' : 'N/A';
    document.getElementById('hist-events').textContent = data.highCpuEvents || 0;
    document.getElementById('hist-peak-mem').textContent =
      data.peakMemoryLast24h ? Math.round(data.peakMemoryLast24h / 1073741824 * 10) / 10 + ' GB' : 'N/A';

    const snaps = data.recentSnapshots || [];
    const tbody = document.getElementById('history-tbody');
    tbody.innerHTML = snaps.map(s => `
      <tr>
        <td style="color:var(--text-dim)">${s.recordedAt?.replace('T', ' ').substring(0, 19) || '---'}</td>
        <td style="color:${s.cpuUsage > 85 ? 'var(--danger)' : 'var(--accent)'}">${s.cpuUsage}%</td>
        <td>${s.usedMemory ? Math.round(s.usedMemory / 1073741824 * 10) / 10 + ' GB' : '---'}</td>
        <td>${s.activeConnections ?? '---'}</td>
        <td>${s.hostname || '---'}</td>
      </tr>
    `).join('') || '<tr><td colspan="5" style="color:var(--text-dim)">No snapshots yet — waits 30s to collect first data</td></tr>';

  } catch (e) {
    document.getElementById('history-tbody').innerHTML =
      '<tr><td colspan="5" style="color:var(--text-dim)">Connect to MySQL & start backend to see history</td></tr>';
  }
}

// ─────────────────────────────────────────
// MOCK DATA (when backend is offline)
// ─────────────────────────────────────────
function showMockData() {
  const cpu = (30 + Math.random() * 40).toFixed(1);
  const mem = (50 + Math.random() * 30).toFixed(1);

  document.getElementById('cpu-val').textContent = cpu + '%';
  document.getElementById('cpu-cores').textContent = '8 cores · 16 logical';
  document.getElementById('mem-val').textContent = mem + '%';
  document.getElementById('mem-detail').textContent = `${(mem * 0.16).toFixed(1)} / 16.0 GB`;
  document.getElementById('disk-val').textContent = '62%';
  document.getElementById('disk-detail').textContent = '311 / 512 GB';
  document.getElementById('net-conn').textContent = '42 conn';
  document.getElementById('net-detail').textContent = '↑ 2.4 GB ↓ 8.1 GB';
  document.getElementById('hostname').textContent = 'BACKEND OFFLINE - MOCK DATA';
  document.getElementById('local-ip').textContent = '127.0.0.1';

  setBar('cpu-bar', parseFloat(cpu));
  setBar('mem-bar', parseFloat(mem));
  setBar('disk-bar', 62);
  setBar('net-bar', 21);
  updateCharts(parseFloat(cpu), parseFloat(mem));

  const coresMock = Array.from({length: 8}, () => +(Math.random() * 80).toFixed(1));
  updateCores(coresMock);
}

// ─────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────
function formatBytes(bytes) {
  if (!bytes) return '0 B';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
  if (bytes < 1073741824) return (bytes / 1048576).toFixed(1) + ' MB';
  return (bytes / 1073741824).toFixed(2) + ' GB';
}

// ─────────────────────────────────────────
// BOOTSTRAP
// ─────────────────────────────────────────
refreshDashboard();
setInterval(refreshDashboard, 3000);  // Auto-refresh every 3 seconds