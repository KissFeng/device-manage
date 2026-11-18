/**
 * 光影资产 AI 智能体悬浮窗组件（Spring AI + DeepSeek）
 * 支持角色感知工具调用、Codex 风格折叠思考轨迹、流式渐隐拖尾与会话持久化管理
 */
(function () {
    const token = localStorage.getItem('token');
    let userInfo = null;
    try {
        userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null');
    } catch (e) {
        userInfo = null;
    }

    // 未登录页面（如登录页）不渲染悬浮智能体
    if (!token || !userInfo) {
        return;
    }

    const baseUrl = typeof API_BASE_URL !== 'undefined' ? API_BASE_URL : 'http://localhost:9011';
    const isAdmin = userInfo.role === 'ADMIN' || userInfo.role === 'ROOT';
    const userKey = String(userInfo.id || userInfo.username || 'default');
    const SESSIONS_KEY = `ai_sessions_${userKey}`;
    const ACTIVE_KEY = `ai_active_session_${userKey}`;
    const TOKEN_KEY = `ai_last_token_${userKey}`;

    // 动态注入样式表与 Logo 路径
    const isSubDir = window.location.pathname.includes('/admin/') || window.location.pathname.includes('/user/');
    const cssHref = isSubDir ? '../css/ai-assistant.css' : 'css/ai-assistant.css';
    const logoSrc = isSubDir ? '../static/ai-logo.png' : 'static/ai-logo.png';
    const linkEl = document.createElement('link');
    linkEl.rel = 'stylesheet';
    linkEl.href = cssHref;
    document.head.appendChild(linkEl);

    let sessions = [];
    let activeSessionId = null;
    let isStreaming = false;
    let suggestions = [];

    function loadSessionsFromStorage() {
        try {
            sessions = JSON.parse(localStorage.getItem(SESSIONS_KEY) || '[]');
            if (!Array.isArray(sessions)) sessions = [];
        } catch (e) {
            sessions = [];
        }

        const lastToken = localStorage.getItem(TOKEN_KEY);
        if (lastToken !== token) {
            // 用户重新登录或切换账号：更新 token 并开启全新对话（历史仍保留在抽屉中）
            localStorage.setItem(TOKEN_KEY, token);
            const fresh = createEmptySession();
            sessions.unshift(fresh);
            activeSessionId = fresh.id;
            saveSessions();
        } else {
            const savedActive = localStorage.getItem(ACTIVE_KEY);
            const found = sessions.find(s => s.id === savedActive);
            if (found) {
                activeSessionId = found.id;
            } else if (sessions.length > 0) {
                activeSessionId = sessions[0].id;
            } else {
                const fresh = createEmptySession();
                sessions.unshift(fresh);
                activeSessionId = fresh.id;
                saveSessions();
            }
        }
    }

    function createEmptySession() {
        return {
            id: 's_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7),
            title: '新对话',
            updatedAt: Date.now(),
            messages: []
        };
    }

    function getActiveSession() {
        let s = sessions.find(item => item.id === activeSessionId);
        if (!s) {
            s = createEmptySession();
            sessions.unshift(s);
            activeSessionId = s.id;
        }
        return s;
    }

    function saveSessions() {
        try {
            // 仅保留最近 30 条会话
            if (sessions.length > 30) {
                sessions = sessions.slice(0, 30);
            }
            localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions));
            localStorage.setItem(ACTIVE_KEY, activeSessionId || '');
        } catch (e) {
            // 忽略存储配额异常
        }
    }

    function escapeHtml(str) {
        return String(str || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    // 完整轻量 Markdown 渲染（标题、引用、代码块、分割线、加粗、行内代码、列表、表格）
    function renderMarkdown(md) {
        if (!md) return '';
        const lines = md.replace(/\r\n/g, '\n').split('\n');
        let html = '';
        let inUl = false;
        let inOl = false;
        let inTable = false;
        let inCode = false;
        let codeLines = [];

        const closeLists = () => {
            if (inUl) { html += '</ul>'; inUl = false; }
            if (inOl) { html += '</ol>'; inOl = false; }
        };
        const closeTable = () => {
            if (inTable) { html += '</tbody></table>'; inTable = false; }
        };

        const inlineFormat = (text) => escapeHtml(text)
            .replace(/`([^`]+)`/g, '<code>$1</code>')
            .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

        for (let i = 0; i < lines.length; i++) {
            const raw = lines[i];
            const line = raw.trim();

            if (line.startsWith('```')) {
                closeLists();
                closeTable();
                if (!inCode) {
                    inCode = true;
                    codeLines = [];
                } else {
                    html += `<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`;
                    inCode = false;
                }
                continue;
            }
            if (inCode) {
                codeLines.push(raw);
                continue;
            }

            if (!line) {
                closeLists();
                closeTable();
                continue;
            }

            const headingMatch = line.match(/^(#{1,4})\s+(.+)$/);
            if (headingMatch) {
                closeLists();
                closeTable();
                const level = headingMatch[1].length;
                html += `<h${level}>${inlineFormat(headingMatch[2])}</h${level}>`;
                continue;
            }

            if (/^(-{3,}|\*{3,})$/.test(line)) {
                closeLists();
                closeTable();
                html += '<hr>';
                continue;
            }

            if (line.startsWith('>')) {
                closeLists();
                closeTable();
                html += `<blockquote>${inlineFormat(line.replace(/^>\s*/, ''))}</blockquote>`;
                continue;
            }

            if (line.startsWith('|') && line.endsWith('|')) {
                closeLists();
                const cells = line.slice(1, -1).split('|').map(c => c.trim());
                if (cells.every(c => /^:?-{2,}:?$/.test(c))) {
                    continue;
                }
                if (!inTable) {
                    html += '<table><thead><tr>' + cells.map(c => `<th>${inlineFormat(c)}</th>`).join('') + '</tr></thead><tbody>';
                    inTable = true;
                } else {
                    html += '<tr>' + cells.map(c => `<td>${inlineFormat(c)}</td>`).join('') + '</tr>';
                }
                continue;
            } else {
                closeTable();
            }

            if (/^[-*•]\s+/.test(line)) {
                if (inOl) { html += '</ol>'; inOl = false; }
                if (!inUl) { html += '<ul>'; inUl = true; }
                html += `<li>${inlineFormat(line.replace(/^[-*•]\s+/, ''))}</li>`;
            } else if (/^\d+\.\s+/.test(line)) {
                if (inUl) { html += '</ul>'; inUl = false; }
                if (!inOl) { html += '<ol>'; inOl = true; }
                html += `<li>${inlineFormat(line.replace(/^\d+\.\s+/, ''))}</li>`;
            } else {
                closeLists();
                html += `<p>${inlineFormat(line)}</p>`;
            }
        }
        if (inCode && codeLines.length > 0) {
            html += `<pre><code>${escapeHtml(codeLines.join('\n'))}</code></pre>`;
        }
        closeLists();
        closeTable();
        return html;
    }

    function applyGradientTail(containerEl, tailLen = 14) {
        const walker = document.createTreeWalker(containerEl, NodeFilter.SHOW_TEXT, null);
        let lastTextNode = null;
        let node;
        while ((node = walker.nextNode())) {
            if (node.nodeValue && node.nodeValue.trim().length > 0) {
                lastTextNode = node;
            }
        }
        if (!lastTextNode) return;
        const text = lastTextNode.nodeValue;
        const splitIdx = Math.max(0, text.length - tailLen);
        const headText = text.slice(0, splitIdx);
        const tailText = text.slice(splitIdx);

        const frag = document.createDocumentFragment();
        if (headText) {
            frag.appendChild(document.createTextNode(headText));
        }
        const span = document.createElement('span');
        span.className = 'stream-gradient-tail';
        span.textContent = tailText;
        frag.appendChild(span);
        lastTextNode.parentNode.replaceChild(frag, lastTextNode);
    }

    function stepIconSvg(step) {
        if (step.status === 'done') {
            return '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"><polyline points="20 6 9 17 4 12"></polyline></svg>';
        }
        if (step.status === 'error') {
            return '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>';
        }
        if (step.icon === 'search') {
            return '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>';
        }
        if (step.icon === 'folder') {
            return '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"></path></svg>';
        }
        return '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="4 17 10 11 4 5"></polyline><line x1="12" y1="19" x2="20" y2="19"></line></svg>';
    }

    function initWidget() {
        loadSessionsFromStorage();

        const fab = document.createElement('button');
        fab.className = 'ai-fab';
        fab.title = '打开光影智助';
        fab.innerHTML = `<img src="${logoSrc}" alt="光影智助">`;

        const userName = userInfo.realName || userInfo.username || '用户';
        const userInitial = userName.charAt(0).toUpperCase();

        const panel = document.createElement('div');
        panel.className = 'ai-panel';
        panel.innerHTML = `
            <div class="ai-drawer-backdrop" id="aiDrawerBackdrop"></div>
            <aside class="ai-drawer" id="aiDrawer">
                <div class="ai-drawer-header">
                    <div class="ai-drawer-brand">
                        <div class="ai-avatar-badge"><img src="${logoSrc}" alt="光影智助"></div>
                        <span>光影智助</span>
                    </div>
                    <button class="ai-drawer-collapse-btn" id="aiDrawerCloseBtn" title="收起历史列表">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="15 18 9 12 15 6"></polyline></svg>
                    </button>
                </div>
                <div class="ai-drawer-new-wrap">
                    <button class="ai-drawer-new-btn" id="aiDrawerNewBtn">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><circle cx="12" cy="12" r="9"></circle><line x1="12" y1="8" x2="12" y2="16"></line><line x1="8" y1="12" x2="16" y2="12"></line></svg>
                        <span>新建对话</span>
                    </button>
                </div>
                <div class="ai-drawer-section">
                    <span>历史记录</span>
                    <button class="ai-drawer-clear-btn" id="aiClearHistoryBtn" title="清空历史记录">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"></path><path d="M10 11v6"></path><path d="M14 11v6"></path></svg>
                    </button>
                </div>
                <div class="ai-drawer-list" id="aiDrawerList"></div>
                <div class="ai-drawer-footer">
                    <div class="ai-drawer-user-dot">${escapeHtml(userInitial)}</div>
                    <span>${escapeHtml(userName)}</span>
                </div>
            </aside>

            <div class="ai-header">
                <div class="ai-header-left">
                    <button class="ai-icon-btn" id="aiMenuBtn" title="历史对话记录">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="3" y1="6" x2="21" y2="6"></line><line x1="3" y1="12" x2="21" y2="12"></line><line x1="3" y1="18" x2="21" y2="18"></line></svg>
                    </button>
                    <div class="ai-avatar-badge">
                        <img src="${logoSrc}" alt="光影智助">
                    </div>
                    <span class="ai-title">光影智助</span>
                </div>
                <div class="ai-header-actions">
                    <button class="ai-icon-btn" id="aiNewChatBtn" title="新建对话">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"></circle><line x1="12" y1="8" x2="12" y2="16"></line><line x1="8" y1="12" x2="16" y2="12"></line></svg>
                    </button>
                    <button class="ai-icon-btn" id="aiExpandBtn" title="展开/收起窗口">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 3 21 3 21 9"></polyline><polyline points="9 21 3 21 3 15"></polyline><line x1="21" y1="3" x2="14" y2="10"></line><line x1="3" y1="21" x2="10" y2="14"></line></svg>
                    </button>
                    <button class="ai-icon-btn" id="aiCloseBtn" title="关闭">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
                    </button>
                </div>
            </div>
            <div class="ai-messages" id="aiMessages"></div>
            <div class="ai-footer">
                <div class="ai-input-box">
                    <textarea class="ai-textarea" id="aiInput" rows="1" placeholder="给 光影智助 发送消息..."></textarea>
                    <div class="ai-input-toolbar">
                        <button class="ai-send-btn" id="aiSendBtn" title="发送" disabled>
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="19" x2="12" y2="5"></line><polyline points="5 12 12 5 19 12"></polyline></svg>
                        </button>
                    </div>
                </div>
                <div class="ai-footer-note">回答基于系统实时数据，请核实重要信息</div>
            </div>
        `;

        document.body.appendChild(fab);
        document.body.appendChild(panel);

        const messagesEl = panel.querySelector('#aiMessages');
        const inputEl = panel.querySelector('#aiInput');
        const sendBtn = panel.querySelector('#aiSendBtn');
        const drawerEl = panel.querySelector('#aiDrawer');
        const backdropEl = panel.querySelector('#aiDrawerBackdrop');
        const drawerListEl = panel.querySelector('#aiDrawerList');

        function toggleDrawer(open) {
            if (open) {
                renderDrawerList();
                drawerEl.classList.add('open');
                backdropEl.classList.add('open');
            } else {
                drawerEl.classList.remove('open');
                backdropEl.classList.remove('open');
            }
        }

        function renderDrawerList() {
            const visibleSessions = sessions.filter(s => s.messages.length > 0 || s.id === activeSessionId);
            if (visibleSessions.length === 0) {
                drawerListEl.innerHTML = '<div class="ai-drawer-empty">暂无历史对话记录</div>';
                return;
            }
            drawerListEl.innerHTML = visibleSessions.map(s => `
                <div class="ai-session-item ${s.id === activeSessionId ? 'active' : ''}" data-id="${escapeHtml(s.id)}">
                    <span class="ai-session-title" title="${escapeHtml(s.title)}">${escapeHtml(s.title)}</span>
                    <button class="ai-session-del" data-del="${escapeHtml(s.id)}" title="删除对话">×</button>
                </div>
            `).join('');

            drawerListEl.querySelectorAll('.ai-session-item').forEach(item => {
                item.addEventListener('click', (e) => {
                    if (e.target.closest('.ai-session-del') || isStreaming) return;
                    activeSessionId = item.getAttribute('data-id');
                    saveSessions();
                    renderActiveSession();
                    toggleDrawer(false);
                });
            });

            drawerListEl.querySelectorAll('.ai-session-del').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    if (isStreaming) return;
                    const delId = btn.getAttribute('data-del');
                    sessions = sessions.filter(s => s.id !== delId);
                    if (sessions.length === 0) {
                        const fresh = createEmptySession();
                        sessions.unshift(fresh);
                        activeSessionId = fresh.id;
                    } else if (activeSessionId === delId) {
                        activeSessionId = sessions[0].id;
                    }
                    saveSessions();
                    renderDrawerList();
                    renderActiveSession();
                });
            });
        }

        function renderWelcome() {
            const greeting = isAdmin
                ? `您好 <strong>${escapeHtml(userName)}</strong>，我是<strong>光影智助</strong>。我可以帮您实时查询全库设备库存、待审批申请与逾期借用情况。`
                : `您好 <strong>${escapeHtml(userName)}</strong>，我是<strong>光影智助</strong>。您可以问我当前有哪些可借用器材、剩余可用数量，或查询您的借用审批进度。`;

            const sugHtml = suggestions.map(q => `
                <button class="ai-suggestion-item" data-q="${escapeHtml(q)}">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748b" stroke-width="2"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
                    <span>${escapeHtml(q)}</span>
                </button>
            `).join('');

            messagesEl.innerHTML = `
                <div class="ai-welcome-card">
                    <div>${greeting}</div>
                    <div style="margin-top:8px;color:#64748b;font-size:12.5px;">您可以参考下面示例发起提问：</div>
                    <div class="ai-suggestions">${sugHtml}</div>
                </div>
            `;

            messagesEl.querySelectorAll('.ai-suggestion-item').forEach(btn => {
                btn.addEventListener('click', () => sendQuestion(btn.getAttribute('data-q')));
            });
        }

        function renderSavedMessage(msg) {
            if (msg.role === 'user') {
                const userRow = document.createElement('div');
                userRow.className = 'ai-msg-row user';
                userRow.innerHTML = `<div class="ai-user-bubble">${escapeHtml(msg.content)}</div>`;
                messagesEl.appendChild(userRow);
                return;
            }
            const aiRow = document.createElement('div');
            aiRow.className = 'ai-msg-row assistant';
            const steps = Array.isArray(msg.steps) ? msg.steps : [];
            const summaryText = `已思考 · ${steps.length} 个步骤 (${msg.elapsedSec || '0.0'}s)`;
            const stepsHtml = steps.map(step => `
                <div class="codex-step-row ${step.status || 'done'}">
                    <div class="codex-step-left">
                        <span class="codex-step-icon">${stepIconSvg(step)}</span>
                        <span class="codex-step-text" title="${escapeHtml(step.title)}">${escapeHtml(step.title)}</span>
                    </div>
                    <span class="codex-step-time">${step.durationMs != null ? step.durationMs + 'ms' : ''}</span>
                </div>
            `).join('');

            aiRow.innerHTML = `
                <div class="ai-assistant-wrap">
                    <div class="codex-trace collapsed" style="${steps.length === 0 ? 'display:none;' : ''}">
                        <button class="codex-trace-toggle" type="button">
                            <span class="codex-status-text">${summaryText} ›</span>
                        </button>
                        <div class="codex-trace-list">${stepsHtml}</div>
                    </div>
                    <div class="ai-answer-body">${renderMarkdown(msg.content)}</div>
                </div>
            `;
            const traceEl = aiRow.querySelector('.codex-trace');
            const toggleBtn = aiRow.querySelector('.codex-trace-toggle');
            const statusTextEl = aiRow.querySelector('.codex-status-text');
            toggleBtn.addEventListener('click', () => {
                traceEl.classList.toggle('collapsed');
                statusTextEl.textContent = traceEl.classList.contains('collapsed')
                    ? `${summaryText} ›`
                    : `${summaryText} ⌄`;
            });
            messagesEl.appendChild(aiRow);
        }

        function renderActiveSession() {
            const session = getActiveSession();
            if (!session.messages || session.messages.length === 0) {
                renderWelcome();
            } else {
                messagesEl.innerHTML = '';
                session.messages.forEach(renderSavedMessage);
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }
        }

        function startNewConversation() {
            if (isStreaming) return;
            const current = getActiveSession();
            if (current.messages.length === 0) {
                renderWelcome();
                toggleDrawer(false);
                return;
            }
            const fresh = createEmptySession();
            sessions.unshift(fresh);
            activeSessionId = fresh.id;
            saveSessions();
            renderActiveSession();
            toggleDrawer(false);
        }

        async function loadSuggestions() {
            try {
                const res = await fetch(`${baseUrl}/ai/suggestions`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                const json = await res.json();
                if (json && json.code === 200 && Array.isArray(json.data)) {
                    suggestions = json.data;
                }
            } catch (e) {
                suggestions = isAdmin
                    ? ['目前全库设备总数、待审批和逾期情况如何？', '帮我列出当前所有待审批的借用申请']
                    : ['现在有哪些相机或镜头可以借用？', '帮我查一下我当前的借用申请和审批状态'];
            }
            renderActiveSession();
        }

        async function sendQuestion(text) {
            const question = (text || inputEl.value || '').trim();
            if (!question || isStreaming) return;

            const session = getActiveSession();
            if (session.messages.length === 0) {
                messagesEl.innerHTML = '';
                session.title = question.length > 22 ? question.slice(0, 22) + '...' : question;
            }
            session.updatedAt = Date.now();
            const historySnapshot = session.messages.slice(-8).map(m => ({ role: m.role, content: m.content }));

            inputEl.value = '';
            inputEl.style.height = 'auto';
            isStreaming = true;
            sendBtn.disabled = true;

            const userRow = document.createElement('div');
            userRow.className = 'ai-msg-row user';
            userRow.innerHTML = `<div class="ai-user-bubble">${escapeHtml(question)}</div>`;
            messagesEl.appendChild(userRow);

            const aiRow = document.createElement('div');
            aiRow.className = 'ai-msg-row assistant';
            aiRow.innerHTML = `
                <div class="ai-assistant-wrap">
                    <div class="codex-trace">
                        <button class="codex-trace-toggle" type="button">
                            <span class="codex-pulse"></span>
                            <span class="codex-status-text">正在思考 ⌄</span>
                        </button>
                        <div class="codex-trace-list"></div>
                    </div>
                    <div class="ai-answer-body"></div>
                </div>
            `;
            messagesEl.appendChild(aiRow);
            messagesEl.scrollTop = messagesEl.scrollHeight;

            const traceEl = aiRow.querySelector('.codex-trace');
            const toggleBtn = aiRow.querySelector('.codex-trace-toggle');
            const statusTextEl = aiRow.querySelector('.codex-status-text');
            const pulseEl = aiRow.querySelector('.codex-pulse');
            const traceListEl = aiRow.querySelector('.codex-trace-list');
            const answerEl = aiRow.querySelector('.ai-answer-body');

            let summaryBaseText = '';
            toggleBtn.addEventListener('click', () => {
                traceEl.classList.toggle('collapsed');
                if (summaryBaseText) {
                    statusTextEl.textContent = traceEl.classList.contains('collapsed')
                        ? `${summaryBaseText} ›`
                        : `${summaryBaseText} ⌄`;
                }
            });

            const stepMap = new Map();
            const stepDataMap = new Map();
            const startedAt = Date.now();
            let fullAnswer = '';

            const upsertStep = (step) => {
                stepDataMap.set(step.id, step);
                let row = stepMap.get(step.id);
                if (!row) {
                    row = document.createElement('div');
                    stepMap.set(step.id, row);
                    traceListEl.appendChild(row);
                }
                row.className = `codex-step-row ${step.status || 'running'}`;
                const timeStr = step.durationMs != null ? `${step.durationMs}ms` : '';
                row.innerHTML = `
                    <div class="codex-step-left">
                        <span class="codex-step-icon">${stepIconSvg(step)}</span>
                        <span class="codex-step-text" title="${escapeHtml(step.title)}">${escapeHtml(step.title)}</span>
                    </div>
                    <span class="codex-step-time">${timeStr}</span>
                `;
                messagesEl.scrollTop = messagesEl.scrollHeight;
            };

            try {
                const resp = await fetch(`${baseUrl}/ai/chat/stream`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify({ message: question, history: historySnapshot })
                });

                if (!resp.ok) {
                    throw new Error(`HTTP ${resp.status}`);
                }

                const reader = resp.body.getReader();
                const decoder = new TextDecoder('utf-8');
                let buffer = '';
                let currentEvent = 'message';

                while (true) {
                    const { value, done } = await reader.read();
                    if (done) break;
                    buffer += decoder.decode(value, { stream: true });

                    const lines = buffer.split('\n');
                    buffer = lines.pop() || '';

                    for (const rawLine of lines) {
                        const line = rawLine.trimEnd();
                        if (line.startsWith('event:')) {
                            currentEvent = line.slice(6).trim();
                        } else if (line.startsWith('data:')) {
                            const dataStr = line.slice(5).trim();
                            if (!dataStr) continue;
                            let payload = null;
                            try { payload = JSON.parse(dataStr); } catch { payload = { text: dataStr }; }

                            if (currentEvent === 'step') {
                                upsertStep(payload);
                            } else if (currentEvent === 'token') {
                                fullAnswer += (payload.text || '');
                                answerEl.innerHTML = renderMarkdown(fullAnswer);
                                applyGradientTail(answerEl, 14);
                                messagesEl.scrollTop = messagesEl.scrollHeight;
                            } else if (currentEvent === 'error') {
                                fullAnswer += `\n\n> ⚠️ ${payload.message || '请求失败'}`;
                                answerEl.innerHTML = renderMarkdown(fullAnswer);
                            }
                        }
                    }
                }

                answerEl.innerHTML = renderMarkdown(fullAnswer);
            } catch (err) {
                fullAnswer = `请求智能体失败：${err.message}`;
                answerEl.innerHTML = renderMarkdown(fullAnswer);
            } finally {
                isStreaming = false;
                sendBtn.disabled = !inputEl.value.trim();
                if (pulseEl) pulseEl.remove();
                const elapsedSec = ((Date.now() - startedAt) / 1000).toFixed(1);
                if (stepMap.size === 0) {
                    traceEl.style.display = 'none';
                } else {
                    traceEl.classList.add('collapsed');
                    summaryBaseText = `已思考 · ${stepMap.size} 个步骤 (${elapsedSec}s)`;
                    statusTextEl.textContent = `${summaryBaseText} ›`;
                }

                // 持久化当前会话消息与步骤
                session.messages.push({ role: 'user', content: question });
                session.messages.push({
                    role: 'assistant',
                    content: fullAnswer,
                    steps: Array.from(stepDataMap.values()),
                    elapsedSec
                });
                session.updatedAt = Date.now();
                saveSessions();
                messagesEl.scrollTop = messagesEl.scrollHeight;
            }
        }

        fab.addEventListener('click', () => {
            panel.classList.add('open');
            fab.style.display = 'none';
            inputEl.focus();
        });

        panel.querySelector('#aiCloseBtn').addEventListener('click', () => {
            panel.classList.remove('open');
            fab.style.display = 'flex';
            toggleDrawer(false);
        });

        panel.querySelector('#aiExpandBtn').addEventListener('click', () => {
            panel.classList.toggle('expanded');
        });

        panel.querySelector('#aiMenuBtn').addEventListener('click', () => toggleDrawer(true));
        panel.querySelector('#aiDrawerCloseBtn').addEventListener('click', () => toggleDrawer(false));
        backdropEl.addEventListener('click', () => toggleDrawer(false));

        panel.querySelector('#aiNewChatBtn').addEventListener('click', startNewConversation);
        panel.querySelector('#aiDrawerNewBtn').addEventListener('click', startNewConversation);

        panel.querySelector('#aiClearHistoryBtn').addEventListener('click', () => {
            if (isStreaming) return;
            sessions = [createEmptySession()];
            activeSessionId = sessions[0].id;
            saveSessions();
            renderDrawerList();
            renderActiveSession();
        });

        const inputBoxEl = panel.querySelector('.ai-input-box');
        inputBoxEl.addEventListener('click', (e) => {
            if (!e.target.closest('#aiSendBtn')) {
                inputEl.focus();
            }
        });

        sendBtn.addEventListener('click', () => sendQuestion());
        inputEl.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendQuestion();
            }
        });
        inputEl.addEventListener('input', () => {
            inputEl.style.height = 'auto';
            inputEl.style.height = Math.min(inputEl.scrollHeight, 96) + 'px';
            if (!isStreaming) {
                sendBtn.disabled = !inputEl.value.trim();
            }
        });

        loadSuggestions();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initWidget);
    } else {
        initWidget();
    }
})();
