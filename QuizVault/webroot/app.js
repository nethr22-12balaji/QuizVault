let token = null;
let currentRole = null;
let syncInterval = null;
let currentUser = null;

const escapeHtml = (unsafe) => {
    return (unsafe || '').toString()
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
};

function nav(viewId) {
    document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
    document.getElementById(viewId).classList.add('active');
    if (syncInterval) { clearInterval(syncInterval); syncInterval = null; }
    
    if (viewId === 'question-bank-view') fetchQuestions();
    if (viewId === 'quiz-setup-view') fetchSetupData();
    if (viewId === 'manage-teams-view') fetchTeams();
    if (viewId === 'archives-view') fetchArchives();
    if (viewId === 'certificates-view') fetchCertificates();
    if (viewId === 'live-quiz-qm-view' || viewId === 'live-quiz-participant-view') pollState();
    
    document.getElementById('msg-banner').style.display = 'none';
}

function navBack() {
    nav(currentRole === 'Quizmaster' ? 'quizmaster-dashboard-view' : 'participant-dashboard-view');
}

function showMsg(id, msg, isError = false) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = msg;
    el.style.color = isError ? 'var(--red)' : 'var(--green)';
}

async function api(endpoint, data = null) {
    const options = { 
        method: data ? 'POST' : 'GET', 
        headers: {} 
    };
    if (data) {
        options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
        options.body = new URLSearchParams(data).toString();
    }
    if (token) options.headers['X-Token'] = token;
    
    try {
        const res = await fetch('/api/' + endpoint, options);
        if (res.status === 401 || res.status === 403) {
            logout();
            return { success: false, error: "Session expired or unauthorized." };
        }
        return await res.json();
    } catch (e) {
        return { success: false, error: "Connection error." };
    }
}

// --- AUTH ---
function toggleAuth() {
    const isLogin = document.getElementById('auth-title').textContent === 'Login';
    document.getElementById('auth-title').textContent = isLogin ? 'Register' : 'Login';
    document.getElementById('auth-btn').textContent = isLogin ? 'REGISTER' : 'LOGIN';
    document.getElementById('auth-toggle').textContent = isLogin ? 'Already have an account? Login' : 'Need an account? Register';
    showMsg('login-error', '');
}

async function authAction() {
    const isLogin = document.getElementById('auth-title').textContent === 'Login';
    const u = document.getElementById('login-username').value;
    const p = document.getElementById('login-password').value;
    const r = document.getElementById('login-role').value;
    
    const res = await api(isLogin ? 'login' : 'register', { username: u, password: p, role: r });
    if (res.success) {
        if (isLogin) {
            token = res.token; currentRole = res.role; currentUser = u;
            document.getElementById('main-header').style.display = 'flex';
            document.getElementById('logged-in-user').textContent = u + ' (' + r + ')';
            
            document.querySelectorAll('.qm-only').forEach(e => e.style.display = (r === 'Quizmaster' ? 'block' : 'none'));
            document.querySelectorAll('.part-only').forEach(e => e.style.display = (r === 'Participant' ? 'block' : 'none'));
            
            nav(r === 'Quizmaster' ? 'quizmaster-dashboard-view' : 'participant-dashboard-view');
        } else {
            toggleAuth();
            showMsg('login-error', 'Registration successful. Please log in.');
        }
    } else {
        showMsg('login-error', res.error, true);
    }
}

function logout() {
    token = null; currentRole = null; currentUser = null;
    document.getElementById('main-header').style.display = 'none';
    if (syncInterval) { clearInterval(syncInterval); syncInterval = null; }
    nav('login-view');
}

// --- QUESTION BANK ---
async function fetchQuestions() {
    const res = await api('questions');
    const el = document.getElementById('qb-list');
    el.innerHTML = '';
    
    let filter = document.getElementById('qb-search').value.toLowerCase();
    
    if (!res.questions || res.questions.length === 0) {
        el.innerHTML = '<div class="list-item" style="color:var(--text-muted)">No questions yet.</div>';
        return;
    }
    
    res.questions.forEach(q => {
        if (filter && !q.text.toLowerCase().includes(filter) && !q.tags.toLowerCase().includes(filter) && !q.difficulty.toLowerCase().includes(filter)) return;
        const cls = q.difficulty === 'Easy' ? 'badge-easy' : q.difficulty === 'Medium' ? 'badge-medium' : 'badge-hard';
        
        const div = document.createElement('div');
        div.className = 'list-item';
        div.innerHTML = `
            <div>
                <strong>${escapeHtml(q.text)}</strong><br>
                <span style="color:var(--text-muted);font-size:0.85rem">Ans: ${escapeHtml(q.answer)} | Tags: ${escapeHtml(q.tags)}</span>
            </div>
            <div style="display:flex;gap:10px;align-items:center;">
                <span class="badge ${cls}">${escapeHtml(q.difficulty)}</span>
                ${currentRole === 'Quizmaster' ? `<button class="btn-small btn-danger del-btn">X</button>` : ''}
            </div>
        `;
        if (currentRole === 'Quizmaster') {
            div.querySelector('.del-btn').addEventListener('click', async () => {
                if (confirm("Delete this question?")) {
                    await api('questions/delete', { text: q.text });
                    fetchQuestions();
                }
            });
        }
        el.appendChild(div);
    });
}

async function addQuestion() {
    const t = document.getElementById('qb-q').value;
    const a = document.getElementById('qb-a').value;
    const tg = document.getElementById('qb-tags').value;
    const d = document.getElementById('qb-diff').value;
    const res = await api('questions/add', { text: t, answer: a, tags: tg, diff: d });
    if (res.success) {
        document.getElementById('qb-q').value = '';
        document.getElementById('qb-a').value = '';
        document.getElementById('qb-tags').value = '';
        showMsg('qb-msg', 'Question added!');
        fetchQuestions();
    } else {
        showMsg('qb-msg', res.error, true);
    }
}

// --- TEAMS ---
async function fetchTeams() {
    const res = await api('teams');
    const el = document.getElementById('teams-list');
    el.innerHTML = '';
    if (!res.teams || res.teams.length === 0) {
        el.innerHTML = '<div class="list-item" style="color:var(--text-muted)">No teams created yet.</div>';
        return;
    }
    res.teams.forEach(t => {
        const div = document.createElement('div');
        div.className = 'list-item';
        div.innerHTML = `
            <div><strong>${escapeHtml(t.name)}</strong><br>
            <span style="color:var(--text-muted);font-size:0.85rem">${escapeHtml(t.players.join(', '))}</span></div>
            <button class="btn-small join-btn">JOIN</button>
        `;
        div.querySelector('.join-btn').addEventListener('click', async () => {
            const jr = await api('teams/join', { name: t.name });
            if (jr.success) { alert("Joined!"); fetchTeams(); } else alert(jr.error);
        });
        el.appendChild(div);
    });
}

async function createTeam() {
    const n = document.getElementById('team-new-name').value;
    const res = await api('teams/create', { name: n });
    if (res.success) {
        document.getElementById('team-new-name').value = '';
        fetchTeams();
    } else alert(res.error);
}

// --- SETUP ---
let bankStats = { Easy:0, Medium:0, Hard:0 };
async function fetchSetupData() {
    const qs = await api('questions');
    bankStats = { Easy:0, Medium:0, Hard:0 };
    if (qs.questions) qs.questions.forEach(q => bankStats[q.difficulty] = (bankStats[q.difficulty] || 0) + 1);
    
    document.getElementById('lbl-easy').textContent = `Easy (${bankStats.Easy} in bank)`;
    document.getElementById('lbl-med').textContent = `Medium (${bankStats.Medium} in bank)`;
    document.getElementById('lbl-hard').textContent = `Hard (${bankStats.Hard} in bank)`;
    
    document.getElementById('qs-easy').max = bankStats.Easy;
    document.getElementById('qs-medium').max = bankStats.Medium;
    document.getElementById('qs-hard').max = bankStats.Hard;
    
    const dp = document.getElementById('qs-date');
    if (!dp.value) dp.valueAsDate = new Date();

    const res = await api('teams');
    const el = document.getElementById('qs-teams-list');
    el.innerHTML = '';
    if (res.teams) {
        res.teams.forEach(t => {
            const li = document.createElement('li');
            li.innerHTML = `<input type="checkbox" value="${escapeHtml(t.name)}">
                            <div><strong>${escapeHtml(t.name)}</strong> <span style="color:var(--text-muted);font-size:0.85rem">— ${escapeHtml(t.players.join(', '))}</span></div>`;
            li.addEventListener('click', () => {
                const cb = li.querySelector('input');
                cb.checked = !cb.checked;
                cb.checked ? li.classList.add('selected') : li.classList.remove('selected');
            });
            li.querySelector('input').addEventListener('click', e => e.stopPropagation());
            el.appendChild(li);
        });
    }
}

async function startQuiz() {
    const checks = document.querySelectorAll('#qs-teams-list input[type="checkbox"]:checked');
    const teams = Array.from(checks).map(c => c.value);
    
    const res = await api('quiz/start', {
        name: document.getElementById('qs-name').value,
        date: document.getElementById('qs-date').value,
        teams: teams.join(','),
        easy: document.getElementById('qs-easy').value || '0',
        med: document.getElementById('qs-medium').value || '0',
        hard: document.getElementById('qs-hard').value || '0'
    });
    
    if (res.success) navLiveQM();
    else alert(res.error);
}

// --- LIVE ---
function navLiveQM() {
    nav('live-quiz-qm-view');
    pollState();
}

function navLiveParticipant() {
    nav('live-quiz-participant-view');
    document.getElementById('live-p-answer').addEventListener('keypress', e => {
        if (e.key === 'Enter') submitAnswer();
    });
    pollState();
}

async function submitAnswer() {
    const input = document.getElementById('live-p-answer');
    const ans = input.value;
    if (!ans) return;
    const res = await api('quiz/submit', { answer: ans });
    if (res.success) {
        input.value = "";
        showMsg('p-feedback', res.message + " (" + (res.delta > 0 ? "+"+res.delta : res.delta) + ")", !res.correct);
    } else {
        showMsg('p-feedback', res.error, true);
    }
}

function pollState() {
    if (syncInterval) clearInterval(syncInterval);
    syncInterval = setInterval(async () => {
        try {
            const res = await api('quiz/state');
            document.getElementById('msg-banner').style.display = 'none';
            if (currentRole === 'Quizmaster') renderQM(res);
            else renderPart(res);
        } catch (e) {
            document.getElementById('msg-banner').textContent = "Connection lost. Retrying...";
            document.getElementById('msg-banner').style.display = 'block';
        }
    }, 1000);
}

function renderQM(res) {
    if (!res.active) {
        document.getElementById('live-qm-qbox').innerHTML = '<h3 style="color:var(--text)">No active quiz.</h3>';
        if (res.results) {
            let s = "<h3>Final Results</h3><ul>";
            res.results.forEach(r => s += `<li>${escapeHtml(r.name)} - ${r.score}</li>`);
            s += "</ul>";
            document.getElementById('live-qm-qbox').innerHTML += s;
        }
        return;
    }
    
    document.getElementById('live-qm-eventname').textContent = res.name;
    
    let html = "";
    for (const [tName, data] of Object.entries(res.teams)) {
        html += `<div class="card" style="padding:16px;margin-bottom:10px;">
            <div style="display:flex;justify-content:space-between;margin-bottom:8px">
                <strong>${escapeHtml(tName)}</strong>
                <span class="badge ${data.finished ? 'badge-easy' : 'badge-medium'}">
                    ${data.finished ? 'Finished' : `Q ${data.qnum} of ${data.total} (${escapeHtml(data.round)})`}
                </span>
            </div>
            ${!data.finished ? `
                <div style="font-size:1.1rem;margin-bottom:8px">${escapeHtml(data.question)}</div>
                <div style="color:var(--text-muted);font-size:0.9rem;margin-bottom:8px">Ans: <strong>${escapeHtml(data.answer)}</strong></div>
                <button class="btn-small" data-skip-team="${escapeHtml(tName)}">Skip</button>
            ` : `<div style="color:var(--green)">Waiting for others...</div>`}
        </div>`;
    }
    document.getElementById('live-qm-qbox').innerHTML = html;
    
    document.querySelectorAll('[data-skip-team]').forEach(btn => {
        btn.addEventListener('click', () => api('quiz/skip', { team: btn.getAttribute('data-skip-team') }));
    });
    
    document.getElementById('live-qm-log').textContent = (res.log || []).join('\n');
    
    let scoresArr = Object.entries(res.teams).map(([name, data]) => ({name, score: data.score}));
    scoresArr.sort((a,b) => b.score - a.score);
    const tbody = document.querySelector('#live-qm-scores tbody');
    tbody.innerHTML = scoresArr.map(s => `<tr><td>${escapeHtml(s.name)}</td><td style="color:var(--green);font-weight:800">${s.score}</td></tr>`).join('');
}

function renderPart(res) {
    const qbox = document.getElementById('live-p-qbox');
    const input = document.getElementById('live-p-answer');
    
    if (!res.active) {
        qbox.innerHTML = "No active quiz.";
        input.disabled = true;
        if (res.results) {
            let s = "<h3>Final Results</h3><ul>";
            res.results.forEach(r => s += `<li>${escapeHtml(r.name)} - <strong style="color:var(--green)">${r.score}</strong></li>`);
            s += "</ul>";
            qbox.innerHTML += s;
        }
        return;
    }
    
    let myTeamName = res.userTeams ? res.userTeams[currentUser] : null;
    let myTeamData = myTeamName && res.teams ? res.teams[myTeamName] : null;
    
    document.getElementById('live-p-teamname').textContent = myTeamName || "Not in a team";
    
    if (!myTeamData) {
        qbox.innerHTML = "You are not participating in this quiz.";
        input.disabled = true;
    } else if (myTeamData.finished) {
        qbox.innerHTML = "<div style='color:var(--green)'>Your team has finished, waiting for others...</div>";
        input.disabled = true;
    } else {
        qbox.innerHTML = `
            <div style="color:var(--text-muted);margin-bottom:10px">
                Question ${myTeamData.qnum} of ${myTeamData.total} - ${escapeHtml(myTeamData.round)} Round
            </div>
            <div style="font-size:1.5rem">${escapeHtml(myTeamData.question)}</div>
        `;
        input.disabled = false;
    }
    
    let scoresArr = Object.entries(res.teams).map(([name, data]) => ({name, score: data.score}));
    scoresArr.sort((a,b) => b.score - a.score);
    const ul = document.getElementById('live-p-scores');
    ul.innerHTML = scoresArr.map(s => `<li><span>${escapeHtml(s.name)}</span><span class="score-val">${s.score}</span></li>`).join('');
}

async function finishQuiz() {
    if (confirm("Are you sure you want to finish the quiz?")) {
        await api('quiz/finish');
        navBack();
    }
}

// --- ARCHIVES ---
async function fetchArchives() {
    const res = await api('archives');
    const el = document.getElementById('archives-list');
    el.innerHTML = '';
    if (!res.archives || res.archives.length === 0) { el.innerHTML = "No archives found."; return; }
    res.archives.forEach(a => {
        const div = document.createElement('div');
        div.className = 'list-item';
        div.innerHTML = `<span>${escapeHtml(a.file)}</span> <button class="btn-small view-arch-btn">View</button>`;
        div.querySelector('.view-arch-btn').addEventListener('click', () => viewArchive(a.file));
        el.appendChild(div);
    });
}

async function viewArchive(filename) {
    const res = await api('archives/detail', { file: filename });
    if (res.name) {
        let s = `<h2>${escapeHtml(res.name)} (${escapeHtml(res.date)})</h2><ul>`;
        res.teams.forEach(t => s += `<li>${escapeHtml(t.name)} : <strong>${t.score}</strong></li>`);
        s += `</ul>`;
        document.getElementById('archive-detail').innerHTML = s;
    }
}

async function fetchCertificates() {
    const res = await api('certificates');
    const el = document.getElementById('certs-list');
    el.innerHTML = '';
    if (!res.certificates || res.certificates.length === 0) { el.innerHTML = "No certificates found."; return; }
    res.certificates.forEach(c => {
        const div = document.createElement('div');
        div.className = 'list-item';
        div.innerHTML = `
            <div><strong>${escapeHtml(c.event)}</strong><br>
            <span style="color:var(--text-muted);font-size:0.85rem">${escapeHtml(c.team)} - ${escapeHtml(c.date)}</span></div>
            <a href="${escapeHtml(c.file)}" target="_blank" class="btn-small">View / Print</a>
        `;
        el.appendChild(div);
    });
}
