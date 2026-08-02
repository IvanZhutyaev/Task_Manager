const TOKEN_KEY = 'tm.web.token';
const API_KEY = 'tm.web.apiBase';

const state = {
  token: localStorage.getItem(TOKEN_KEY),
  user: null,
  view: 'projects',
  projects: [],
  organizations: [],
  selectedProjectId: null,
  selectedProject: null,
  selectedOrgId: null,
  selectedOrg: null,
  orgMembers: [],
  orgTeams: [],
  teamMembersById: {},
  members: [],
  boards: [],
  columnsByBoard: {},
  tasksByColumn: {},
  eventSources: [],
};

const els = {
  authView: document.getElementById('authView'),
  appView: document.getElementById('appView'),
  userBar: document.getElementById('userBar'),
  userLabel: document.getElementById('userLabel'),
  logoutBtn: document.getElementById('logoutBtn'),
  loginForm: document.getElementById('loginForm'),
  registerForm: document.getElementById('registerForm'),
  authError: document.getElementById('authError'),
  apiBase: document.getElementById('apiBase'),
  projectsView: document.getElementById('projectsView'),
  orgsView: document.getElementById('orgsView'),
  projectList: document.getElementById('projectList'),
  orgList: document.getElementById('orgList'),
  emptyState: document.getElementById('emptyState'),
  projectDetail: document.getElementById('projectDetail'),
  projectTitle: document.getElementById('projectTitle'),
  projectMeta: document.getElementById('projectMeta'),
  boardsArea: document.getElementById('boardsArea'),
  projectSidePanel: document.getElementById('projectSidePanel'),
  searchResults: document.getElementById('searchResults'),
  orgEmpty: document.getElementById('orgEmpty'),
  orgDetail: document.getElementById('orgDetail'),
  orgTitle: document.getElementById('orgTitle'),
  orgMeta: document.getElementById('orgMeta'),
  orgMembersList: document.getElementById('orgMembersList'),
  teamsArea: document.getElementById('teamsArea'),
  addOrgMemberForm: document.getElementById('addOrgMemberForm'),
  addTeamForm: document.getElementById('addTeamForm'),
  newProjectBtn: document.getElementById('newProjectBtn'),
  newOrgBtn: document.getElementById('newOrgBtn'),
  newBoardBtn: document.getElementById('newBoardBtn'),
  inviteMemberBtn: document.getElementById('inviteMemberBtn'),
  runSearch: document.getElementById('runSearch'),
  loadNotifs: document.getElementById('loadNotifs'),
  projectSearch: document.getElementById('projectSearch'),
  invitesBanner: document.getElementById('invitesBanner'),
  toast: document.getElementById('toast'),
  modal: document.getElementById('modal'),
  modalForm: document.getElementById('modalForm'),
  modalTitle: document.getElementById('modalTitle'),
  modalFields: document.getElementById('modalFields'),
  modalError: document.getElementById('modalError'),
};

els.apiBase.value = localStorage.getItem(API_KEY) || 'http://localhost:8080/api/v1';

function apiBase() {
  const value = els.apiBase.value.replace(/\/$/, '');
  localStorage.setItem(API_KEY, value);
  return value;
}

function toast(message) {
  els.toast.hidden = false;
  els.toast.textContent = message;
  clearTimeout(toast._t);
  toast._t = setTimeout(() => { els.toast.hidden = true; }, 2800);
}

async function api(path, options = {}) {
  const headers = {
    Accept: 'application/json',
    ...(options.body ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  const res = await fetch(`${apiBase()}${path}`, { ...options, headers });
  if (res.status === 204) return null;
  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = { message: text }; }
  }
  if (!res.ok) {
    if (res.status === 401) logout(false);
    throw new Error(data?.message || `HTTP ${res.status}`);
  }
  return data;
}

function setToken(token) {
  state.token = token;
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

function logout(redirect = true) {
  closeBoardEvents();
  setToken(null);
  Object.assign(state, {
    user: null, projects: [], organizations: [],
    selectedProjectId: null, selectedProject: null,
    selectedOrgId: null, selectedOrg: null,
  });
  if (redirect) showAuth();
}

function showAuth() {
  els.authView.hidden = false;
  els.appView.hidden = true;
  els.userBar.hidden = true;
}

function showApp() {
  els.authView.hidden = true;
  els.appView.hidden = false;
  els.userBar.hidden = false;
  els.userLabel.textContent = state.user ? `${state.user.name} · ${state.user.email}` : '';
  setView(state.view || 'projects');
}

function setView(view) {
  state.view = view;
  document.querySelectorAll('.nav-btn').forEach((b) => {
    b.classList.toggle('active', b.dataset.view === view);
  });
  els.projectsView.hidden = view !== 'projects';
  els.orgsView.hidden = view !== 'organizations';
}

document.querySelectorAll('.nav-btn').forEach((btn) => {
  btn.addEventListener('click', () => setView(btn.dataset.view));
});

document.querySelectorAll('.tab').forEach((tab) => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach((t) => t.classList.remove('active'));
    tab.classList.add('active');
    const isLogin = tab.dataset.tab === 'login';
    els.loginForm.hidden = !isLogin;
    els.registerForm.hidden = isLogin;
    els.authError.textContent = '';
  });
});

async function afterAuth(data) {
  setToken(data.token);
  state.user = data.user;
  showApp();
  await refreshHome();
}

els.loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  els.authError.textContent = '';
  const fd = new FormData(els.loginForm);
  try {
    await afterAuth(await api('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email: fd.get('email'), password: fd.get('password') }),
    }));
  } catch (err) { els.authError.textContent = err.message; }
});

els.registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  els.authError.textContent = '';
  const fd = new FormData(els.registerForm);
  try {
    await afterAuth(await api('/auth/register', {
      method: 'POST',
      body: JSON.stringify({
        name: fd.get('name'), email: fd.get('email'), password: fd.get('password'),
      }),
    }));
  } catch (err) { els.authError.textContent = err.message; }
});

els.logoutBtn.addEventListener('click', () => logout());

function memberOptions(includeEmpty = true) {
  const opts = includeEmpty ? [{ value: '', label: 'Без исполнителя' }] : [];
  state.members.forEach((m) => opts.push({ value: String(m.userId), label: `${m.name} (${m.email})` }));
  return opts;
}

async function refreshHome() {
  state.projects = await api('/projects');
  renderProjects();
  await loadOrganizations();
  await loadMyInvitationsBanner();
  if (state.selectedProjectId) await openProject(state.selectedProjectId);
  else {
    els.emptyState.hidden = false;
    els.projectDetail.hidden = true;
  }
}

async function loadOrganizations() {
  try { state.organizations = await api('/organizations'); }
  catch { state.organizations = []; }
  renderOrganizations();
  if (state.selectedOrgId) await openOrganization(state.selectedOrgId);
}

function renderProjects() {
  els.projectList.innerHTML = '';
  if (!state.projects.length) {
    els.projectList.innerHTML = '<li class="rail-hint" style="padding:0.5rem">Проектов пока нет</li>';
    return;
  }
  state.projects.forEach((p) => {
    const li = document.createElement('li');
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = p.id === state.selectedProjectId ? 'active' : '';
    const org = p.organizationId
      ? (state.organizations.find((o) => o.id === p.organizationId)?.name || `org #${p.organizationId}`)
      : 'Личный';
    btn.innerHTML = `<strong>${escapeHtml(p.name)}</strong><small>${escapeHtml(p.currentUserRole || '')} · ${escapeHtml(org)}</small>`;
    btn.addEventListener('click', () => openProject(p.id));
    li.appendChild(btn);
    els.projectList.appendChild(li);
  });
}

function renderOrganizations() {
  els.orgList.innerHTML = '';
  if (!state.organizations.length) {
    els.orgList.innerHTML = '<li class="rail-hint" style="padding:0.5rem">Создайте первую организацию</li>';
    return;
  }
  state.organizations.forEach((o) => {
    const li = document.createElement('li');
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = o.id === state.selectedOrgId ? 'active' : '';
    btn.innerHTML = `<strong>${escapeHtml(o.name)}</strong><small>${escapeHtml(o.type)} · ваша роль: ${escapeHtml(o.currentUserRole || '—')}</small>`;
    btn.addEventListener('click', () => {
      setView('organizations');
      openOrganization(o.id);
    });
    li.appendChild(btn);
    els.orgList.appendChild(li);
  });
}

async function openOrganization(orgId) {
  state.selectedOrgId = orgId;
  state.selectedOrg = state.organizations.find((o) => o.id === orgId)
    || await api(`/organizations/${orgId}`);
  renderOrganizations();
  els.orgEmpty.hidden = true;
  els.orgDetail.hidden = false;
  els.orgTitle.textContent = state.selectedOrg.name;
  els.orgMeta.textContent = `${state.selectedOrg.type} · роль ${state.selectedOrg.currentUserRole || '—'} · лимиты: ${state.selectedOrg.maxMembers ?? '—'} уч. / ${state.selectedOrg.maxTeams ?? '—'} команд`;

  const [members, teams] = await Promise.all([
    api(`/organizations/${orgId}/members`),
    api(`/organizations/${orgId}/teams`),
  ]);
  state.orgMembers = members || [];
  state.orgTeams = teams || [];
  state.teamMembersById = {};
  await Promise.all((state.orgTeams || []).map(async (team) => {
    try {
      state.teamMembersById[team.id] = await api(`/organizations/${orgId}/teams/${team.id}/members`);
    } catch {
      state.teamMembersById[team.id] = [];
    }
  }));
  renderOrgMembers();
  renderTeams();
}

function renderOrgMembers() {
  els.orgMembersList.innerHTML = state.orgMembers.length
    ? state.orgMembers.map((m) => `
      <li>
        <span><strong>${escapeHtml(m.name || m.email)}</strong><br><small>${escapeHtml(m.email)}</small></span>
        <span class="role-pill">${escapeHtml(m.role)}</span>
      </li>`).join('')
    : '<li class="rail-hint">Пока никого нет — добавьте участника формой выше</li>';
}

function renderTeams() {
  if (!state.orgTeams.length) {
    els.teamsArea.innerHTML = '<p class="rail-hint">Команд пока нет. Создайте первую формой выше — потом добавьте в неё людей.</p>';
    return;
  }
  els.teamsArea.innerHTML = '';
  state.orgTeams.forEach((team) => {
    const card = document.createElement('article');
    card.className = 'team-card';
    const members = state.teamMembersById[team.id] || [];
    card.innerHTML = `
      <h4>${escapeHtml(team.name)} <small class="role-pill">#${team.id}</small></h4>
      <form class="mini-form" data-team="${team.id}">
        <input name="email" type="email" required placeholder="email участника организации" />
        <button class="btn small primary" type="submit">В команду</button>
      </form>
      <ul>${members.length
        ? members.map((m) => `<li>${escapeHtml(m.name || m.email)} · ${escapeHtml(m.email || '')}</li>`).join('')
        : '<li>В команде пока пусто</li>'}</ul>`;
    card.querySelector('form').addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = new FormData(e.target).get('email');
      try {
        await api(`/organizations/${state.selectedOrgId}/teams/${team.id}/members`, {
          method: 'POST',
          body: JSON.stringify({ email }),
        });
        toast(`Добавлен в команду «${team.name}»`);
        e.target.reset();
        await openOrganization(state.selectedOrgId);
      } catch (err) { toast(err.message); }
    });
    els.teamsArea.appendChild(card);
  });
}

els.addOrgMemberForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!state.selectedOrgId) return;
  const fd = new FormData(e.target);
  try {
    await api(`/organizations/${state.selectedOrgId}/members`, {
      method: 'POST',
      body: JSON.stringify({ email: fd.get('email'), role: fd.get('role') || 'MEMBER' }),
    });
    toast('Участник организации добавлен');
    e.target.reset();
    await openOrganization(state.selectedOrgId);
  } catch (err) { toast(err.message); }
});

els.addTeamForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!state.selectedOrgId) return;
  const fd = new FormData(e.target);
  try {
    await api(`/organizations/${state.selectedOrgId}/teams`, {
      method: 'POST',
      body: JSON.stringify({ name: fd.get('name') }),
    });
    toast('Команда создана');
    e.target.reset();
    await openOrganization(state.selectedOrgId);
  } catch (err) { toast(err.message); }
});

async function loadMyInvitationsBanner() {
  try {
    const invites = await api('/users/me/invitations');
    const pending = (invites || []).filter((i) => i.status === 'PENDING');
    if (!pending.length) {
      els.invitesBanner.hidden = true;
      els.invitesBanner.innerHTML = '';
      return;
    }
    els.invitesBanner.hidden = false;
    els.invitesBanner.innerHTML = `<strong>Входящие приглашения в проекты</strong>
      <ul class="people-list" style="margin-top:0.6rem">${pending.map((inv) => `
        <li>
          <span>${escapeHtml(inv.projectName || `Проект #${inv.projectId}`)} → ${escapeHtml(inv.role)}</span>
          <span class="row">
            <button type="button" class="btn small primary" data-accept="${inv.id}">Принять</button>
            <button type="button" class="btn small ghost" data-decline="${inv.id}">Отклонить</button>
          </span>
        </li>`).join('')}</ul>`;
    els.invitesBanner.querySelectorAll('[data-accept]').forEach((b) => {
      b.addEventListener('click', async () => {
        await api(`/invitations/${b.dataset.accept}/accept`, { method: 'POST' });
        toast('Приглашение принято');
        await refreshHome();
      });
    });
    els.invitesBanner.querySelectorAll('[data-decline]').forEach((b) => {
      b.addEventListener('click', async () => {
        await api(`/invitations/${b.dataset.decline}/decline`, { method: 'POST' });
        await loadMyInvitationsBanner();
      });
    });
  } catch {
    els.invitesBanner.hidden = true;
  }
}

async function openProject(projectId) {
  setView('projects');
  state.selectedProjectId = projectId;
  let project = state.projects.find((p) => p.id === projectId);
  if (!project) {
    project = await api(`/projects/${projectId}`);
    state.projects = await api('/projects');
  }
  state.selectedProject = project;
  renderProjects();
  els.emptyState.hidden = true;
  els.projectDetail.hidden = false;
  els.projectTitle.textContent = project.name;
  const orgName = project.organizationId
    ? (state.organizations.find((o) => o.id === project.organizationId)?.name || `#${project.organizationId}`)
    : 'личный проект';
  els.projectMeta.textContent = `${project.description || 'Без описания'} · роль ${project.currentUserRole} · ${orgName}`;

  state.members = await api(`/projects/${projectId}/members`);
  state.boards = await api(`/projects/${projectId}/boards`);
  state.columnsByBoard = {};
  state.tasksByColumn = {};
  state.orgTeams = [];
  if (project.organizationId) {
    try { state.orgTeams = await api(`/organizations/${project.organizationId}/teams`); }
    catch { state.orgTeams = []; }
  }

  for (const board of state.boards) {
    const columns = await api(`/boards/${board.id}/columns`);
    state.columnsByBoard[board.id] = columns;
    for (const col of columns) {
      const page = await api(`/columns/${col.id}/tasks?size=50`);
      state.tasksByColumn[col.id] = page.content || [];
    }
  }
  renderBoards();
  subscribeBoardEvents(state.boards.map((b) => b.id));
  await renderProjectSidePanel();
}

function closeBoardEvents() {
  (state.eventSources || []).forEach((s) => { try { s.close(); } catch { /* ignore */ } });
  state.eventSources = [];
}

function subscribeBoardEvents(boardIds) {
  closeBoardEvents();
  if (!state.token || !boardIds?.length) return;
  boardIds.forEach((boardId) => {
    try {
      const url = `${apiBase()}/boards/${boardId}/events?access_token=${encodeURIComponent(state.token)}`;
      const source = new EventSource(url);
      source.addEventListener('board', () => {
        if (state.selectedProjectId) openProject(state.selectedProjectId).catch(() => {});
      });
      state.eventSources.push(source);
    } catch { /* ignore */ }
  });
}

els.inviteMemberBtn.addEventListener('click', () => {
  if (!state.selectedProjectId) return;
  openModal('Пригласить в проект', [
    { name: 'email', label: 'Email', required: true },
    { name: 'role', label: 'Роль', type: 'select', options: ['EDITOR', 'VIEWER', 'CONTRACTOR'], value: 'EDITOR' },
  ], async (values) => {
    await api(`/projects/${state.selectedProjectId}/invitations`, {
      method: 'POST',
      body: JSON.stringify({ email: values.email, role: values.role }),
    });
    toast('Приглашение отправлено');
    await renderProjectSidePanel();
  });
});

els.runSearch.addEventListener('click', async () => {
  const q = els.projectSearch.value.trim();
  try {
    const page = await api(`/projects/${state.selectedProjectId}/tasks?q=${encodeURIComponent(q)}&size=50`);
    const items = page.content || [];
    els.searchResults.hidden = false;
    els.searchResults.innerHTML = items.length
      ? `<strong>Найдено: ${items.length}</strong><ul class="people-list" style="margin-top:0.5rem">${items.map((t) =>
        `<li><span>#${t.id} ${escapeHtml(t.title)}</span><span class="role-pill">${escapeHtml(t.status)}</span></li>`).join('')}</ul>`
      : 'Ничего не найдено';
  } catch (err) { toast(err.message); }
});

els.loadNotifs.addEventListener('click', async () => {
  try {
    const list = await api('/users/me/notifications');
    els.searchResults.hidden = false;
    els.searchResults.innerHTML = (list || []).length
      ? `<strong>Уведомления</strong><ul class="people-list" style="margin-top:0.5rem">${(list || []).slice(0, 15).map((n) =>
        `<li><span>${escapeHtml(n.message || n.type)}</span><span class="role-pill">${n.read ? 'read' : 'new'}</span></li>`).join('')}</ul>`
      : 'Нет уведомлений';
  } catch (err) { toast(err.message); }
});

async function renderProjectSidePanel() {
  const [labels, sprints, activityPage] = await Promise.all([
    api(`/projects/${state.selectedProjectId}/labels`),
    api(`/projects/${state.selectedProjectId}/sprints`),
    api(`/projects/${state.selectedProjectId}/activity?size=10`),
  ]);
  const activity = activityPage.content || activityPage || [];
  els.projectSidePanel.innerHTML = `
    <section class="block">
      <h3>Участники проекта</h3>
      <ul class="people-list">${state.members.map((m) =>
        `<li><span>${escapeHtml(m.name)} · ${escapeHtml(m.email)}</span><span class="role-pill">${escapeHtml(m.role)}</span></li>`).join('') || '<li>нет</li>'}</ul>
    </section>
    <section class="block">
      <h3>Метки и спринты</h3>
      <p>${(labels || []).map((l) => `<span class="badge">${escapeHtml(l.name)}</span>`).join(' ') || 'Меток нет'}</p>
      <div class="row" style="margin:0.6rem 0">
        <button type="button" class="btn small" id="addLabelBtn">+ Метка</button>
        <button type="button" class="btn small" id="addSprintBtn">+ Спринт</button>
      </div>
      <ul class="people-list">${(sprints || []).map((s) =>
        `<li><span>${escapeHtml(s.name)}</span><span class="role-pill">${escapeHtml(s.status)}</span></li>`).join('') || '<li>Спринтов нет</li>'}</ul>
    </section>
    <section class="block" style="grid-column:1/-1">
      <h3>Активность</h3>
      <ul class="people-list">${activity.map((a) =>
        `<li><span>${escapeHtml(a.actorName || '')}: ${escapeHtml(a.action)}</span></li>`).join('') || '<li>пусто</li>'}</ul>
    </section>`;

  els.projectSidePanel.querySelector('#addLabelBtn')?.addEventListener('click', () => {
    openModal('Новая метка', [
      { name: 'name', label: 'Название', required: true },
      { name: 'color', label: 'Цвет (#hex)', value: '#0f766e' },
    ], async (values) => {
      await api(`/projects/${state.selectedProjectId}/labels`, {
        method: 'POST',
        body: JSON.stringify({ name: values.name, color: values.color || '#0f766e' }),
      });
      await renderProjectSidePanel();
    });
  });
  els.projectSidePanel.querySelector('#addSprintBtn')?.addEventListener('click', () => {
    openModal('Новый спринт', [
      { name: 'name', label: 'Название', required: true },
    ], async (values) => {
      await api(`/projects/${state.selectedProjectId}/sprints`, {
        method: 'POST',
        body: JSON.stringify({ name: values.name, startDate: null, endDate: null }),
      });
      await renderProjectSidePanel();
    });
  });
}

function renderBoards() {
  els.boardsArea.innerHTML = '';
  if (!state.boards.length) {
    els.boardsArea.innerHTML = '<p class="empty">В проекте пока нет досок. Нажмите «+ Доска».</p>';
    return;
  }
  state.boards.forEach((board) => {
    const section = document.createElement('section');
    section.className = 'board';
    const columns = state.columnsByBoard[board.id] || [];
    section.innerHTML = `
      <div class="board-head">
        <h3>${escapeHtml(board.name)} <span class="badge">${escapeHtml(board.accessMode || 'OPEN')}</span></h3>
        <div class="row">
          <button type="button" class="btn small ghost" data-acl="${board.id}">Доступ</button>
          <button type="button" class="btn small" data-add-col="${board.id}">+ Колонка</button>
        </div>
      </div>
      <div class="columns"></div>`;
    const colsEl = section.querySelector('.columns');
    columns.forEach((col) => {
      const colEl = document.createElement('div');
      colEl.className = 'column';
      const tasks = state.tasksByColumn[col.id] || [];
      colEl.innerHTML = `
        <div class="col-head">
          <strong>${escapeHtml(col.name)}${col.wipLimit != null ? ` · WIP ${col.wipLimit}` : ''}</strong>
          <button type="button" class="btn small" data-add-task="${col.id}">+ Задача</button>
        </div>`;
      tasks.forEach((task) => colEl.appendChild(renderTask(task, columns)));
      colsEl.appendChild(colEl);
    });

    section.querySelector('[data-add-col]').addEventListener('click', () => {
      openModal('Новая колонка', [
        { name: 'name', label: 'Название', required: true },
        { name: 'wipLimit', label: 'WIP limit (опц.)', inputType: 'number' },
        { name: 'mappedStatus', label: 'Статус при переносе', type: 'select',
          options: [{ value: '', label: '—' }, 'BACKLOG', 'IN_PROGRESS', 'DONE', 'ARCHIVED'] },
      ], async (values) => {
        await api(`/boards/${board.id}/columns`, {
          method: 'POST',
          body: JSON.stringify({
            name: values.name,
            wipLimit: values.wipLimit ? Number(values.wipLimit) : null,
            mappedStatus: values.mappedStatus || null,
          }),
        });
        await openProject(state.selectedProjectId);
      });
    });

    section.querySelector('[data-acl]')?.addEventListener('click', () => {
      const teamOpts = [
        { value: '', label: '—' },
        ...(state.orgTeams || []).map((t) => ({ value: String(t.id), label: t.name })),
      ];
      openModal(`Доступ к доске «${board.name}»`, [
        { name: 'accessMode', label: 'Режим', type: 'select', options: ['OPEN', 'PRIVATE', 'TEAM_ACL'], value: board.accessMode || 'OPEN' },
        { name: 'teamId', label: 'Команда (для TEAM_ACL)', type: 'select', options: teamOpts,
          value: board.teamIds?.[0] != null ? String(board.teamIds[0]) : '' },
      ], async (values) => {
        await api(`/projects/${state.selectedProjectId}/boards/${board.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            name: board.name,
            accessMode: values.accessMode,
            teamIds: values.accessMode === 'TEAM_ACL' && values.teamId ? [Number(values.teamId)] : [],
          }),
        });
        toast('Доступ обновлён');
        await openProject(state.selectedProjectId);
      });
    });

    section.querySelectorAll('[data-add-task]').forEach((btn) => {
      btn.addEventListener('click', () => openTaskEditor(Number(btn.dataset.addTask)));
    });
    els.boardsArea.appendChild(section);
  });
}

function openTaskEditor(columnId, existing = null) {
  const isEdit = !!existing;
  openModal(isEdit ? `Задача #${existing.id}` : 'Новая задача', [
    { name: 'title', label: 'Заголовок', required: true, value: existing?.title || '' },
    { name: 'description', label: 'Описание', value: existing?.description || '' },
    { name: 'priority', label: 'Приоритет', type: 'select', options: ['LOW', 'MEDIUM', 'HIGH'], value: existing?.priority || 'MEDIUM' },
    { name: 'status', label: 'Статус', type: 'select', options: ['BACKLOG', 'IN_PROGRESS', 'DONE', 'ARCHIVED'], value: existing?.status || 'BACKLOG' },
    { name: 'deadline', label: 'Дедлайн', inputType: 'date', value: existing?.deadline || '' },
    { name: 'assigneeId', label: 'Исполнитель', type: 'select', options: memberOptions(true),
      value: existing?.assigneeId != null ? String(existing.assigneeId) : '' },
    { name: 'taskType', label: 'Тип', type: 'select',
      options: [{ value: '', label: '—' }, 'BUG', 'FEATURE', 'CHORE'], value: existing?.taskType || '' },
  ], async (values) => {
    const payload = {
      title: values.title,
      description: values.description || null,
      priority: values.priority,
      status: values.status || null,
      deadline: values.deadline || null,
      assigneeId: values.assigneeId ? Number(values.assigneeId) : null,
      taskType: values.taskType || null,
      estimateHours: null,
      spentHours: null,
      sprintId: existing?.sprintId ?? null,
      labelIds: null,
    };
    if (isEdit) await api(`/tasks/${existing.id}`, { method: 'PUT', body: JSON.stringify(payload) });
    else await api(`/columns/${columnId}/tasks`, { method: 'POST', body: JSON.stringify(payload) });
    await openProject(state.selectedProjectId);
  });
}

function renderTask(task, columns) {
  const el = document.createElement('article');
  el.className = 'task';
  el.innerHTML = `
    <h4>${escapeHtml(task.title)}</h4>
    <p>${escapeHtml(task.description || '')}</p>
    <div class="badges">
      <span class="badge ${escapeHtml(task.priority)}">${escapeHtml(task.priority)}</span>
      <span class="badge">${escapeHtml(task.status)}</span>
      ${task.assigneeName ? `<span class="badge">${escapeHtml(task.assigneeName)}</span>` : ''}
    </div>
    <div class="task-actions"></div>`;
  const actions = el.querySelector('.task-actions');
  const editBtn = document.createElement('button');
  editBtn.type = 'button';
  editBtn.textContent = 'Изменить';
  editBtn.addEventListener('click', async () => {
    try { openTaskEditor((await api(`/tasks/${task.id}`)).columnId, await api(`/tasks/${task.id}`)); }
    catch (err) { toast(err.message); }
  });
  actions.appendChild(editBtn);

  const extrasBtn = document.createElement('button');
  extrasBtn.type = 'button';
  extrasBtn.textContent = 'Коммент / чеклист';
  extrasBtn.addEventListener('click', () => openTaskExtras(task.id));
  actions.appendChild(extrasBtn);

  columns.filter((c) => c.id !== task.columnId).forEach((c) => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = `→ ${c.name}`;
    b.addEventListener('click', async () => {
      try {
        await api(`/tasks/${task.id}/move`, { method: 'PATCH', body: JSON.stringify({ columnId: c.id }) });
        await openProject(state.selectedProjectId);
      } catch (err) { toast(err.message); }
    });
    actions.appendChild(b);
  });
  return el;
}

async function openTaskExtras(taskId) {
  openModal('Комментарий или пункт чеклиста', [
    { name: 'action', label: 'Действие', type: 'select', options: [
      { value: 'comment', label: 'Добавить комментарий' },
      { value: 'checklist', label: 'Добавить пункт чеклиста' },
      { value: 'suggest-dod', label: 'Подсказать DoD' },
    ], value: 'comment' },
    { name: 'text', label: 'Текст (для комментария / пункта)' },
  ], async (values) => {
    if (values.action === 'suggest-dod') {
      const suggestion = await api(`/tasks/${taskId}/ai/suggest-dod`, { method: 'POST', body: '{}' });
      await api(`/tasks/${taskId}/checklist/bulk`, {
        method: 'POST',
        body: JSON.stringify((suggestion.items || []).map((i) => ({ title: i.title, done: false }))),
      });
      toast('DoD добавлен в чеклист');
      return;
    }
    if (!values.text) throw new Error('Введите текст');
    if (values.action === 'comment') {
      await api(`/tasks/${taskId}/comments`, { method: 'POST', body: JSON.stringify({ body: values.text }) });
      toast('Комментарий добавлен');
    } else {
      await api(`/tasks/${taskId}/checklist`, { method: 'POST', body: JSON.stringify({ title: values.text, done: false }) });
      toast('Пункт чеклиста добавлен');
    }
  });
}

els.newOrgBtn.addEventListener('click', () => {
  openModal('Новая организация', [
    { name: 'name', label: 'Название', required: true },
    { name: 'type', label: 'Тип', type: 'select', options: ['LOCAL', 'COMMERCIAL'], value: 'LOCAL' },
  ], async (values) => {
    const org = await api('/organizations', {
      method: 'POST',
      body: JSON.stringify({ name: values.name, type: values.type || 'LOCAL' }),
    });
    toast('Организация создана');
    await loadOrganizations();
    setView('organizations');
    await openOrganization(org.id);
  });
});

els.newProjectBtn.addEventListener('click', () => {
  const orgOpts = [
    { value: '', label: 'Личный (без организации)' },
    ...(state.organizations || []).map((o) => ({ value: String(o.id), label: o.name })),
  ];
  openModal('Новый проект', [
    { name: 'name', label: 'Название', required: true },
    { name: 'description', label: 'Описание' },
    { name: 'organizationId', label: 'Организация', type: 'select', options: orgOpts, value: '' },
    { name: 'withDefaultBoard', label: 'Стартовая доска Todo/Doing/Done', type: 'select',
      options: [{ value: 'true', label: 'Да' }, { value: 'false', label: 'Нет' }], value: 'true' },
    { name: 'strictBusinessRules', label: 'Строгие правила', type: 'select',
      options: [{ value: 'false', label: 'Нет' }, { value: 'true', label: 'Да' }], value: 'false' },
  ], async (values) => {
    const project = await api('/projects', {
      method: 'POST',
      body: JSON.stringify({
        name: values.name,
        description: values.description || null,
        organizationId: values.organizationId ? Number(values.organizationId) : null,
        strictBusinessRules: values.strictBusinessRules === 'true',
        withDefaultBoard: values.withDefaultBoard === 'true',
      }),
    });
    toast('Проект создан');
    await refreshHome();
    await openProject(project.id);
  });
});

els.newBoardBtn.addEventListener('click', () => {
  if (!state.selectedProjectId) return;
  const teamOpts = [
    { value: '', label: '—' },
    ...(state.orgTeams || []).map((t) => ({ value: String(t.id), label: t.name })),
  ];
  openModal('Новая доска', [
    { name: 'name', label: 'Название', required: true },
    { name: 'accessMode', label: 'Доступ', type: 'select', options: ['OPEN', 'PRIVATE', 'TEAM_ACL'], value: 'OPEN' },
    { name: 'teamId', label: 'Команда (если TEAM_ACL)', type: 'select', options: teamOpts, value: '' },
  ], async (values) => {
    const body = { name: values.name, accessMode: values.accessMode || 'OPEN' };
    if (values.accessMode === 'TEAM_ACL' && values.teamId) body.teamIds = [Number(values.teamId)];
    await api(`/projects/${state.selectedProjectId}/boards`, { method: 'POST', body: JSON.stringify(body) });
    toast('Доска создана');
    await openProject(state.selectedProjectId);
  });
});

let modalSubmit = null;

function normalizeOptions(options) {
  return (options || []).map((opt) => (opt && typeof opt === 'object' ? opt : { value: opt, label: String(opt) }));
}

function openModal(title, fields, onSubmit) {
  els.modalTitle.textContent = title;
  els.modalError.textContent = '';
  els.modalFields.innerHTML = '';
  fields.forEach((f) => {
    const label = document.createElement('label');
    label.append(f.label);
    let input;
    if (f.type === 'select') {
      input = document.createElement('select');
      input.name = f.name;
      normalizeOptions(f.options).forEach((opt) => {
        const o = document.createElement('option');
        o.value = opt.value;
        o.textContent = opt.label;
        if (String(opt.value) === String(f.value ?? '')) o.selected = true;
        input.appendChild(o);
      });
    } else {
      input = document.createElement('input');
      input.name = f.name;
      input.type = f.inputType || 'text';
      if (f.required) input.required = true;
      if (f.value != null && f.value !== '') input.value = f.value;
    }
    label.appendChild(input);
    els.modalFields.appendChild(label);
  });
  modalSubmit = onSubmit;
  els.modal.showModal();
}

els.modalForm.addEventListener('submit', async (e) => {
  if (e.submitter?.value === 'cancel') { modalSubmit = null; return; }
  e.preventDefault();
  if (!modalSubmit) return;
  const values = Object.fromEntries(new FormData(els.modalForm).entries());
  try {
    await modalSubmit(values);
    els.modal.close();
    modalSubmit = null;
  } catch (err) { els.modalError.textContent = err.message; }
});

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

async function boot() {
  if (!state.token) { showAuth(); return; }
  try {
    state.user = await api('/users/me');
    showApp();
    await refreshHome();
  } catch { logout(); }
}

boot();
