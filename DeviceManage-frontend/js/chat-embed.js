/**
 * 光影资产 AI 智能体悬浮窗组件（Spring AI + DeepSeek）
 * 仅在用户已登录后自动加载于页面右下角
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

    // 动态注入样式表与 Logo 路径
    const isSubDir = window.location.pathname.includes('/admin/') || window.location.pathname.includes('/user/');
    const cssHref = isSubDir ? '../css/ai-assistant.css' : 'css/ai-assistant.css';
    const logoSrc = isSubDir ? '../static/ai-logo.png' : 'static/ai-logo.png';
    const linkEl = document.createElement('link');
    linkEl.rel = 'stylesheet';
    linkEl.href = cssHref;
    document.head.appendChild(linkEl);

    const history = [];
    let isStreaming = false;
    let suggestions = [];

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

            // 标题 # ~ ####
            const headingMatch = line.match(/^(#{1,4})\s+(.+)$/);
            if (headingMatch) {
                closeLists();
                closeTable();
                const level = headingMatch[1].length;
                html += `<h${level}>${inlineFormat(headingMatch[2])}</h${level}>`;
                continue;
            }

            // 分割线
            if (/^(-{3,}|\*{3,})$/.test(line)) {
                closeLists();
                closeTable();
                html += '<hr>';
                continue;
            }

            // 引用块 >
            if (line.startsWith('>')) {
                closeLists();
                closeTable();
                html += `<blockquote>${inlineFormat(line.replace(/^>\s*/, ''))}</blockquote>`;
                continue;
            }

            // Markdown 表格行
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

    // 为流式渲染的最后一个文本节点附加从实到虚的横向渐变拖尾
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
        const fab = document.createElement('button');
        fab.className = 'ai-fab';
        fab.title = '打开光影智助';
        fab.innerHTML = `<img src="${logoSrc}" alt="光影智助">`;

        const panel = document.createElement('div');
        panel.className = 'ai-panel';
        panel.innerHTML = `
            <div class="ai-header">
                <div class="ai-header-left">
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

        function renderWelcome() {
            const greeting = isAdmin
                ? `您好 <strong>${escapeHtml(userInfo.realName || userInfo.username)}</strong>，我是<strong>光影智助</strong>。我可以帮您实时查询全库设备库存、待审批申请与逾期借用情况。`
                : `您好 <strong>${escapeHtml(userInfo.realName || userInfo.username)}</strong>，我是<strong>光影智助</strong>。您可以问我当前有哪些可借用器材、剩余可用数量，或查询您的借用审批进度。`;

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
            if (history.length === 0) {
                renderWelcome();
            }
        }

        async function sendQuestion(text) {
            const question = (text || inputEl.value || '').trim();
            if (!question || isStreaming) return;

            if (history.length === 0) {
                messagesEl.innerHTML = '';
            }
            inputEl.value = '';
            inputEl.style.height = 'auto';
            isStreaming = true;
            sendBtn.disabled = true;

            // 1. 渲染用户气泡
            const userRow = document.createElement('div');
            userRow.className = 'ai-msg-row user';
            userRow.innerHTML = `<div class="ai-user-bubble">${escapeHtml(question)}</div>`;
            messagesEl.appendChild(userRow);

            // 2. 渲染 AI 回复容器（含 Codex 风格思考轨迹 + 回答正文）
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
            const startedAt = Date.now();
            let fullAnswer = '';

            const upsertStep = (step) => {
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
                    body: JSON.stringify({ message: question, history: history.slice(-8) })
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

                // 流式输出完成后移除尾部渐变遮罩，恢复清晰显示
                answerEl.innerHTML = renderMarkdown(fullAnswer);
                history.push({ role: 'user', content: question });
                if (fullAnswer) {
                    history.push({ role: 'assistant', content: fullAnswer });
                }
            } catch (err) {
                answerEl.innerHTML = renderMarkdown(`请求智能体失败：${err.message}`);
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
        });

        panel.querySelector('#aiExpandBtn').addEventListener('click', () => {
            panel.classList.toggle('expanded');
        });

        panel.querySelector('#aiNewChatBtn').addEventListener('click', () => {
            if (isStreaming) return;
            history.length = 0;
            renderWelcome();
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
