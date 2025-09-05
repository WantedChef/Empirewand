/* eslint-disable @typescript-eslint/no-var-requires */
const vscode = require('vscode');
const fs = require('fs');
const path = require('path');

/** @param {vscode.ExtensionContext} context */
function activate(context) {
  const cfg = () => vscode.workspace.getConfiguration('codex');

  const historyProvider = new SimpleListProvider(() => historyItems.map(text => ({ label: text })), 'History');
  const logsProvider = new SimpleListProvider(() => logItems.map(text => ({ label: text })), 'Logs');

  context.subscriptions.push(
    vscode.commands.registerCommand('codexPlus.openSettings', () => openSettingsPanel(context)),
    vscode.commands.registerCommand('codexPlus.selectProfile', async () => {
      const options = ['Low', 'Medium', 'High', 'Creative', 'Strict Code'];
      const pick = await vscode.window.showQuickPick(options, { placeHolder: 'Select Codex profile' });
      if (!pick) return;
      await cfg().update('profile', pick, vscode.ConfigurationTarget.Workspace);
      applyProfilePresets(pick);
      vscode.window.showInformationMessage(`Codex profile set to ${pick}`);
    }),
    vscode.commands.registerCommand('codexPlus.selectModel', async () => {
      const { models } = computeModels();
      const pick = await vscode.window.showQuickPick(models, { placeHolder: 'Select Codex model' });
      if (!pick) return;
      await cfg().update('model', pick, vscode.ConfigurationTarget.Workspace);
      vscode.window.showInformationMessage(`Codex model set to ${pick}`);
    }),
    vscode.commands.registerCommand('codexPlus.toggleAutoApprove', async () => {
      const current = cfg().get('autoApprove', true);
      await cfg().update('autoApprove', !current, vscode.ConfigurationTarget.Workspace);
      await syncCodexConfig();
      vscode.window.showInformationMessage(`Auto-approve ${!current ? 'enabled' : 'disabled'}`);
    }),
    vscode.commands.registerCommand('codexPlus.refreshHistory', () => historyProvider.refresh()),
    vscode.commands.registerCommand('codexPlus.refreshLogs', () => logsProvider.refresh()),
    vscode.window.registerTreeDataProvider('codexPlus.history', historyProvider),
    vscode.window.registerTreeDataProvider('codexPlus.logs', logsProvider),
    vscode.window.registerWebviewViewProvider('codexPlus.settings', new SettingsViewProvider())
  );
}

function deactivate() {}

function openSettingsPanel(context) {
  const panel = vscode.window.createWebviewPanel(
    'codexPlusSettings',
    'Codex Plus — Settings',
    vscode.ViewColumn.Active,
    { enableScripts: true, retainContextWhenHidden: true }
  );

  panel.webview.html = getWebviewHtml();

  panel.webview.onDidReceiveMessage(async (msg) => {
    try {
      if (msg.type === 'requestState') {
        panel.webview.postMessage({ type: 'state', payload: await getState() });
      }
      if (msg.type === 'saveState') {
        await saveState(msg.payload);
        panel.webview.postMessage({ type: 'saved' });
        vscode.window.showInformationMessage('Codex settings saved.');
      }
      if (msg.type === 'applyProfile') {
        await applyProfilePresets(msg.payload.profile);
        panel.webview.postMessage({ type: 'state', payload: await getState() });
      }
      if (msg.type === 'quickPickModel') {
        const { models } = computeModels();
        panel.webview.postMessage({ type: 'models', payload: models });
      }
    } catch (e) {
      vscode.window.showErrorMessage(`Codex Plus error: ${e?.message || e}`);
    }
  });
}

async function getState() {
  const conf = vscode.workspace.getConfiguration('codex');
  const work = vscode.ConfigurationTarget.Workspace;
  const codexConfig = readCodexConfig();
  const { models, defaults } = computeModels();
  return {
    profile: conf.get('profile', 'Medium'),
    model: conf.get('model', defaults[0]),
    models,
    legacyModels: conf.get('legacyModels', false),
    customModels: conf.get('customModels', []),
    temperature: conf.get('temperature', 0.3),
    maxTokens: conf.get('maxTokens', 4096),
    reasoningBudget: conf.get('reasoningBudget', 30),
    modelEndpoint: conf.get('modelEndpoint', ''),
    runtime: {
      autoApprove: conf.get('autoApprove', true),
      sandboxMode: conf.get('sandboxMode', 'danger-full-access'),
      networkAccess: conf.get('networkAccess', 'enabled'),
      codexConfig,
    },
  };
}

async function saveState(payload) {
  const conf = vscode.workspace.getConfiguration('codex');
  const T = vscode.ConfigurationTarget.Workspace;
  await conf.update('profile', payload.profile, T);
  await conf.update('model', payload.model, T);
  await conf.update('legacyModels', payload.legacyModels, T);
  await conf.update('customModels', payload.customModels || [], T);
  await conf.update('temperature', payload.temperature, T);
  await conf.update('maxTokens', payload.maxTokens, T);
  await conf.update('reasoningBudget', payload.reasoningBudget, T);
  await conf.update('modelEndpoint', payload.modelEndpoint || '', T);
  await conf.update('autoApprove', payload.runtime.autoApprove, T);
  await conf.update('sandboxMode', payload.runtime.sandboxMode, T);
  await conf.update('networkAccess', payload.runtime.networkAccess, T);
  await syncCodexConfig(payload.runtime);
}

async function applyProfilePresets(profile) {
  const conf = vscode.workspace.getConfiguration('codex');
  const T = vscode.ConfigurationTarget.Workspace;
  const presets = {
    'Low': { temperature: 0.1, reasoningBudget: 10, maxTokens: 2048 },
    'Medium': { temperature: 0.3, reasoningBudget: 30, maxTokens: 4096 },
    'High': { temperature: 0.7, reasoningBudget: 60, maxTokens: 8192 },
    'Creative': { temperature: 0.9, reasoningBudget: 80, maxTokens: 8192 },
    'Strict Code': { temperature: 0.0, reasoningBudget: 20, maxTokens: 4096 },
  };
  const p = presets[profile] || presets['Medium'];
  await conf.update('profile', profile, T);
  await conf.update('temperature', p.temperature, T);
  await conf.update('reasoningBudget', p.reasoningBudget, T);
  await conf.update('maxTokens', p.maxTokens, T);
}

function computeModels() {
  const conf = vscode.workspace.getConfiguration('codex');
  const includeLegacy = conf.get('legacyModels', false);
  const custom = conf.get('customModels', []);
  const modern = [
    'o4', 'o4-mini', 'o4-mini-high',
    'o3', 'o3-mini',
    'gpt-4o', 'gpt-4o-mini'
  ];
  const legacy = [
    'gpt-4', 'gpt-3.5-turbo', 'text-davinci-003'
  ];
  const models = Array.from(new Set([ ...modern, ...(includeLegacy ? legacy : []), ...custom ]) );
  return { models, defaults: modern };
}

function getWorkspaceRoot() {
  const folders = vscode.workspace.workspaceFolders;
  if (!folders || folders.length === 0) return undefined;
  return folders[0].uri.fsPath;
}

function getCodexConfigPath() {
  const root = getWorkspaceRoot();
  if (!root) return undefined;
  return path.join(root, 'codex.config.json');
}

function readCodexConfig() {
  try {
    const p = getCodexConfigPath();
    if (!p || !fs.existsSync(p)) return {};
    const raw = fs.readFileSync(p, 'utf8');
    return JSON.parse(raw);
  } catch {
    return {};
  }
}

async function syncCodexConfig(runtime) {
  const conf = vscode.workspace.getConfiguration('codex');
  const autoApprove = runtime?.autoApprove ?? conf.get('autoApprove', true);
  const sandboxMode = runtime?.sandboxMode ?? conf.get('sandboxMode', 'danger-full-access');
  const networkAccess = runtime?.networkAccess ?? conf.get('networkAccess', 'enabled');
  const p = getCodexConfigPath();
  if (!p) return;
  const existing = readCodexConfig();
  const merged = {
    ...existing,
    approval_policy: autoApprove ? 'never' : 'on-request',
    sandbox_mode: sandboxMode,
    network_access: networkAccess,
    auto_apply_patches: !!autoApprove,
  };
  fs.writeFileSync(p, JSON.stringify(merged, null, 2), 'utf8');
}

function getWebviewHtml() {
  const html = `<!DOCTYPE html>
  <html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Codex Plus — Settings</title>
    <style>
      :root { --bg:#0e1116; --panel:#161b22; --text:#dbe2f0; --muted:#9fb0c3; --accent:#7cacf8; --outline:#2b3240; --ok:#34d399; --warn:#f59e0b; }
      html, body { margin:0; padding:0; background:var(--bg); color:var(--text); font-family: Inter, system-ui, -apple-system, Segoe UI, Roboto, Ubuntu, Cantarell, 'Helvetica Neue', Arial, 'Noto Sans', 'Apple Color Emoji', 'Segoe UI Emoji'; }
      .wrap { padding: 16px 24px 32px; max-width: 1100px; margin: 0 auto; }
      h1 { font-size: 18px; margin: 8px 0 16px; letter-spacing: .3px; }
      .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
      .card { background: var(--panel); border: 1px solid var(--outline); border-radius: 10px; padding: 16px; box-shadow: 0 1px 0 rgba(0,0,0,.5); }
      .card h2 { font-size: 14px; color: var(--muted); margin: 0 0 12px; font-weight: 600; text-transform: uppercase; letter-spacing: .6px; }
      label { display:block; font-size: 12px; color: var(--muted); margin: 10px 0 6px; }
      select, input[type="text"], input[type="number"], input[type="range"] { width: 100%; padding: 8px 10px; background: #0f131a; color: var(--text); border:1px solid var(--outline); border-radius: 8px; }
      .row { display:flex; gap:12px; align-items:center; }
      .row > * { flex: 1; }
      .toggle { display:flex; gap:10px; align-items:center; }
      .pill { display:inline-block; padding:6px 10px; border-radius:999px; border:1px solid var(--outline); background:#0f131a; color:var(--muted); }
      .btns { display:flex; gap:10px; justify-content:flex-end; margin-top: 16px; }
      button { background: var(--accent); color:#0c1116; font-weight:600; border:0; border-radius:8px; padding:8px 12px; cursor:pointer; }
      button.ghost { background: transparent; color: var(--text); border:1px solid var(--outline); }
      .hint { font-size: 12px; color: var(--muted); }
      .mono { font-family: ui-monospace, Menlo, Consolas, monospace; }
    </style>
  </head>
  <body>
    <div class="wrap">
      <h1>Codex Plus — Settings</h1>
      <div class="grid">
        <div class="card">
          <h2>Profiles</h2>
          <label>Profile</label>
          <select id="profile">
            <option>Low</option>
            <option selected>Medium</option>
            <option>High</option>
            <option>Creative</option>
            <option>Strict Code</option>
          </select>
          <div class="row">
            <div>
              <label>Temperature <span id="tempVal" class="pill">0.3</span></label>
              <input id="temperature" type="range" min="0" max="2" step="0.1" value="0.3" />
            </div>
            <div>
              <label>Reasoning Budget</label>
              <input id="reasoningBudget" type="number" min="0" max="200" value="30" />
            </div>
            <div>
              <label>Max Tokens</label>
              <input id="maxTokens" type="number" min="128" max="32768" value="4096" />
            </div>
          </div>
          <div class="btns"><button id="applyProfile" class="ghost">Apply Preset</button></div>
        </div>
        <div class="card">
          <h2>Models</h2>
          <label>Model</label>
          <select id="model"></select>
          <div class="toggle"><input type="checkbox" id="legacyModels" /> <label for="legacyModels">Include legacy models</label></div>
          <label>Custom models (comma separated)</label>
          <input id="customModels" type="text" placeholder="e.g. gpt-4, text-davinci-003, my-custom-model" />
          <label>Custom endpoint (optional)</label>
          <input id="modelEndpoint" type="text" class="mono" placeholder="https://api.example.com/v1" />
          <div class="hint">Models list updates when you toggle legacy or edit custom models.</div>
        </div>
        <div class="card">
          <h2>Runtime</h2>
          <div class="toggle"><input type="checkbox" id="autoApprove" checked /> <label for="autoApprove">Auto-approve shell, file, and network actions</label></div>
          <label>Sandbox</label>
          <select id="sandboxMode">
            <option value="workspace-write">Workspace write</option>
            <option value="danger-full-access" selected>Danger — full access</option>
          </select>
          <label>Network Access</label>
          <select id="networkAccess">
            <option value="enabled" selected>Enabled</option>
            <option value="restricted">Restricted</option>
          </select>
          <div class="hint">Mirrored into <span class="mono">codex.config.json</span> for the agent runtime.</div>
        </div>
        <div class="card">
          <h2>About</h2>
          <p class="hint">Codex Plus enhances your Codex workflow with a richer UI for profiles, models (including legacy), and runtime toggles. Changes are saved to workspace settings and codex.config.json.</p>
          <p class="hint">Commands available via Command Palette: <span class="mono">Codex Plus: Open Settings</span>, <span class="mono">Select Profile</span>, <span class="mono">Select Model</span>, <span class="mono">Toggle Auto-Approve</span>.</p>
        </div>
      </div>
      <div class="btns">
        <button id="save">Save</button>
      </div>
    </div>
    <script>
      const vscode = acquireVsCodeApi();
      const els = (id) => document.getElementById(id);
      const $profile = els('profile');
      const $model = els('model');
      const $legacyModels = els('legacyModels');
      const $customModels = els('customModels');
      const $temperature = els('temperature');
      const $tempVal = els('tempVal');
      const $reasoningBudget = els('reasoningBudget');
      const $maxTokens = els('maxTokens');
      const $modelEndpoint = els('modelEndpoint');
      const $autoApprove = els('autoApprove');
      const $sandboxMode = els('sandboxMode');
      const $networkAccess = els('networkAccess');
      const $applyProfile = els('applyProfile');
      const $save = els('save');

      function post(type, payload) { vscode.postMessage({ type, payload }); }
      function setModels(list, current) {
        $model.innerHTML = '';
        list.forEach(m => {
          const o = document.createElement('option');
          o.value = m; o.textContent = m; if (m === current) o.selected = true; $model.appendChild(o);
        });
      }
      function collect() {
        return {
          profile: $profile.value,
          model: $model.value,
          legacyModels: $legacyModels.checked,
          customModels: ($customModels.value||'').split(',').map(s=>s.trim()).filter(Boolean),
          temperature: parseFloat($temperature.value),
          reasoningBudget: parseInt($reasoningBudget.value||'0', 10),
          maxTokens: parseInt($maxTokens.value||'0', 10),
          modelEndpoint: $modelEndpoint.value,
          runtime: {
            autoApprove: $autoApprove.checked,
            sandboxMode: $sandboxMode.value,
            networkAccess: $networkAccess.value
          }
        }
      }
      window.addEventListener('message', (event) => {
        const msg = event.data;
        if (msg.type === 'state') {
          const s = msg.payload;
          $profile.value = s.profile;
          setModels(s.models, s.model);
          $legacyModels.checked = !!s.legacyModels;
          $customModels.value = (s.customModels||[]).join(', ');
          $temperature.value = s.temperature;
          $tempVal.textContent = s.temperature;
          $reasoningBudget.value = s.reasoningBudget;
          $maxTokens.value = s.maxTokens;
          $modelEndpoint.value = s.modelEndpoint||'';
          $autoApprove.checked = !!s.runtime.autoApprove;
          $sandboxMode.value = s.runtime.sandboxMode;
          $networkAccess.value = s.runtime.networkAccess;
        }
        if (msg.type === 'models') {
          setModels(msg.payload, $model.value);
        }
        if (msg.type === 'saved') {
          $save.textContent = 'Saved'; setTimeout(()=> $save.textContent='Save', 1200);
        }
      });
      $legacyModels.addEventListener('change', ()=> post('quickPickModel'));
      $customModels.addEventListener('input', ()=> post('quickPickModel'));
      $temperature.addEventListener('input', ()=> $tempVal.textContent = $temperature.value);
      $applyProfile.addEventListener('click', ()=> post('applyProfile', { profile: $profile.value }));
      $save.addEventListener('click', ()=> post('saveState', collect()));
      post('requestState');
    </script>
  </body>
  </html>`;
  return html;
}

module.exports = { activate, deactivate };
class SettingsViewProvider {
  /** @param {vscode.WebviewView} webviewView */
  resolveWebviewView(webviewView) {
    webviewView.webview.options = { enableScripts: true, retainContextWhenHidden: true };
    webviewView.webview.html = getWebviewHtml();
    webviewView.webview.onDidReceiveMessage(async (msg) => {
      try {
        if (msg.type === 'requestState') {
          webviewView.webview.postMessage({ type: 'state', payload: await getState() });
        }
        if (msg.type === 'saveState') {
          await saveState(msg.payload);
          webviewView.webview.postMessage({ type: 'saved' });
          vscode.window.showInformationMessage('Codex settings saved.');
        }
        if (msg.type === 'applyProfile') {
          await applyProfilePresets(msg.payload.profile);
          webviewView.webview.postMessage({ type: 'state', payload: await getState() });
        }
        if (msg.type === 'quickPickModel') {
          const { models } = computeModels();
          webviewView.webview.postMessage({ type: 'models', payload: models });
        }
      } catch (e) {
        vscode.window.showErrorMessage(`Codex Plus error: ${e?.message || e}`);
      }
    });
    // Initialize state
    setTimeout(async () => {
      webviewView?.webview?.postMessage({ type: 'state', payload: await getState() });
    }, 0);
  }
}

class SimpleListProvider {
  /** @param {() => {label:string}[]} itemsFn */
  constructor(itemsFn, title) {
    this.itemsFn = itemsFn;
    this.title = title;
    this._emitter = new vscode.EventEmitter();
    this.onDidChangeTreeData = this._emitter.event;
  }
  refresh() { this._emitter.fire(); }
  getTreeItem(element) { return new vscode.TreeItem(element.label); }
  getChildren() { return Promise.resolve(this.itemsFn().map(o => new vscode.TreeItem(o.label))); }
}

const historyItems = [];
const logItems = [];

function addHistoryEntry(entry) { historyItems.unshift(entry); if (historyItems.length > 100) historyItems.pop(); }
function addLogEntry(entry) { logItems.unshift(entry); if (logItems.length > 200) logItems.pop(); }

