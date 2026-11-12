'use strict';

// Front servido pelo Spring Boot: todas as chamadas usam a mesma origem da API.
const main = document.querySelector('#content');
const modal = document.querySelector('#modal');
const labels = { ABERTO: 'Aberto', EM_ATENDIMENTO: 'Em atendimento', AGUARDANDO_CLIENTE: 'Aguardando cliente', FECHADO: 'Fechado', BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica', ADMIN: 'Administrador', TECNICO: 'Técnico' };
const statuses = ['ABERTO', 'EM_ATENDIMENTO', 'AGUARDANDO_CLIENTE', 'FECHADO'];
const priorities = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'];
const catalog = {
  clientes: { title: 'Clientes', singular: 'cliente', description: 'As pessoas que contam com o seu atendimento.', icon: 'users', fields: [['nome', 'Nome', 'text', 100, true], ['email', 'E-mail', 'email', 120, true], ['telefone', 'Telefone', 'tel', 20, false]] },
  categorias: { title: 'Categorias', singular: 'categoria', description: 'Organize os chamados por área ou tipo de solicitação.', icon: 'grid', fields: [['nome', 'Nome', 'text', 100, true], ['descricao', 'Descrição', 'textarea', 255, false]] },
  tecnicos: { title: 'Técnicos', singular: 'técnico', description: 'Quem faz parte da equipe de atendimento.', icon: 'tool', fields: [['nome', 'Nome', 'text', 100, true], ['email', 'E-mail', 'email', 120, true], ['perfil', 'Perfil', 'select', ['TECNICO', 'ADMIN'], true]] }
};
let cache = { clientes: [], categorias: [], tecnicos: [] };
let renderVersion = 0;
let toastTimer;
let modalBusy = false;
const paths = {
 dashboard: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
 ticket: '<path d="M4 4h16v5a3 3 0 0 0 0 6v5H4v-5a3 3 0 0 0 0-6Z"/><path d="M14 5v2m0 3v2m0 3v2"/>',
 users: '<circle cx="9" cy="8" r="3"/><path d="M3 20v-2a6 6 0 0 1 12 0v2m1-15a3 3 0 0 1 0 6m2 3a5 5 0 0 1 3 4v2"/>',
 grid: '<rect x="3" y="3" width="7" height="7" rx="2"/><rect x="14" y="3" width="7" height="7" rx="2"/><rect x="3" y="14" width="7" height="7" rx="2"/><rect x="14" y="14" width="7" height="7" rx="2"/>',
 tool: '<path d="M14 6a5 5 0 0 0-6 6l-5 5a2 2 0 0 0 4 4l5-5a5 5 0 0 0 6-6l-4 3-3-3 3-4Z"/>',
 plus: '<path d="M12 5v14M5 12h14"/>', arrow: '<path d="m9 5 7 7-7 7"/>', back: '<path d="m14 5-7 7 7 7"/>',
 edit: '<path d="m15 4 5 5M4 20l5-1L21 7a2 2 0 0 0-5-5L4 14Z"/>',
 trash: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7"/>',
 search: '<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>', close: '<path d="m6 6 12 12M6 18 18 6"/>',
 clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>', check: '<circle cx="12" cy="12" r="9"/><path d="m7 12 3 3 7-7"/>',
 message: '<path d="M21 11a9 9 0 0 1-9 9H4l-3 2 2-6a9 9 0 1 1 18-5Z"/>', eye: '<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>'
};
function icon(name) { return `<svg viewBox="0 0 24 24" aria-hidden="true">${paths[name] || paths.ticket}</svg>`; }
function esc(value) { return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function badge(value) { return `<span class="badge ${esc(value)}">${esc(labels[value] || value)}</span>`; }
function date(value, time = false) {
  if (!value) return '—';
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', ...(time ? { timeStyle: 'short' } : {}) }).format(new Date(value));
}
function nameOf(type, id) { return cache[type].find(item => item.id === id)?.nome || (id ? `${catalog[type].singular} #${id}` : 'Não atribuído'); }
function options(items, selected, placeholder = 'Selecione') {
  return `<option value="">${esc(placeholder)}</option>` + items.map(item => {
    const value = typeof item === 'string' ? item : item.id;
    const label = typeof item === 'string' ? labels[item] || item : item.nome;
    return `<option value="${esc(value)}" ${String(value) === String(selected) ? 'selected' : ''}>${esc(label)}</option>`;
  }).join('');
}
async function api(path, method = 'GET', data) {
  let response;
  try {
    response = await fetch(path, { method, headers: { Accept: 'application/json', ...(data !== undefined ? { 'Content-Type': 'application/json' } : {}) }, ...(data !== undefined ? { body: JSON.stringify(data) } : {}) });
  } catch {
    throw new Error('Não foi possível conectar ao servidor. Verifique se a aplicação está em execução.');
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}));
    const message = response.status >= 500 ? 'Não foi possível concluir a operação. Se estiver excluindo um cadastro, verifique se ele está vinculado a chamados.' : error.erro || (response.status === 404 ? 'Registro não encontrado.' : 'Não foi possível concluir a operação. Verifique os dados informados.');
    const failure = new Error(message);
    failure.details = error.detalhes || [];
    throw failure;
  }
  return response.status === 204 ? null : response.json();
}
async function loadCatalogs() {
  const types = Object.keys(catalog);
  const lists = await Promise.all(types.map(type => api(`/${type}`)));
  types.forEach((type, index) => { cache[type] = lists[index]; });
}
function errorHtml(error) { return `<div class="alert" role="alert">${esc(error.message)}${error.details?.length ? `<ul>${error.details.map(detail => `<li>${esc(detail)}</li>`).join('')}</ul>` : ''}</div>`; }
function showToast(message) {
  const toast = document.querySelector('#toast');
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.hidden = true; }, 4500);
}
function heading(title, description, action = '') { return `<div class="page-heading"><div><p class="eyebrow">CENTRAL DE ATENDIMENTO</p><h1>${esc(title)}</h1><p class="subtitle">${esc(description)}</p></div>${action}</div>`; }
function empty(title, description, action = '', symbol = 'ticket') { return `<div class="empty"><span class="empty-icon">${icon(symbol)}</span><h3>${esc(title)}</h3><p>${esc(description)}</p>${action}</div>`; }
function newButton(type) {
  const text = type === 'chamados' ? 'Novo chamado' : `${type === 'categorias' ? 'Nova' : 'Novo'} ${catalog[type].singular}`;
  return `<button class="btn primary" data-new="${type}">${icon('plus')}${text}</button>`;
}
function navigate(hash) {
  if (location.hash === hash) render(); else location.hash = hash;
}
function ticketTable(rows, compact = false) {
  return `<div class="table-wrap"><table><thead><tr><th>Chamado</th>${compact ? '' : '<th>Cliente</th>'}<th>Status</th><th>Prioridade</th>${compact ? '' : '<th>Abertura</th>'}<th><span class="sr-only">Ações</span></th></tr></thead><tbody>${rows.map(c => `<tr><td><a class="cell-title" href="#chamados/${c.id}"><span class="ticket-id">#${String(c.id).padStart(3, '0')}</span>${esc(c.titulo)}</a><span class="cell-sub">${esc(nameOf('categorias', c.categoriaId))}</span></td>${compact ? '' : `<td>${esc(nameOf('clientes', c.clienteId))}</td>`}<td>${badge(c.status)}</td><td>${badge(c.prioridade)}</td>${compact ? '' : `<td>${date(c.dataAbertura)}</td>`}<td><a class="icon-btn" href="#chamados/${c.id}" aria-label="Abrir chamado ${c.id}">${icon('arrow')}</a></td></tr>`).join('')}</tbody></table></div>`;
}
async function dashboard(version) {
  await loadCatalogs();
  const [recent, ...counts] = await Promise.all([
    api('/chamados?size=5&sort=dataAbertura,desc&sort=id,desc'),
    ...statuses.map(status => api(`/chamados?status=${status}&size=1`))
  ]);
  if (version !== renderVersion) return;
  const total = recent.totalElements;
  const cards = statuses.map((status, i) => `<a class="stat" href="#chamados?status=${status}"><div class="stat-top">${labels[status]}<span class="stat-icon ${['', 'blue', 'amber', 'gray'][i]}">${icon(['ticket', 'tool', 'clock', 'check'][i])}</span></div><span class="stat-value">${counts[i].totalElements}</span><span class="stat-note">${['Prontos para o próximo passo', 'Com atendimento em andamento', 'Aguardando um retorno', 'Atendimentos concluídos'][i]}</span></a>`).join('');
  main.innerHTML = heading('Visão geral', 'Tudo o que você precisa para acompanhar o atendimento.', newButton('chamados')) +
    `<section class="hero"><div><p class="eyebrow">CADA SOLICITAÇÃO IMPORTA</p><h2>Um lugar para organizar e resolver.</h2><p>Acompanhe os chamados, conecte sua equipe e mantenha cada atendimento no caminho certo.</p></div><div class="hero-icon">${icon('ticket')}</div></section><section class="stats" aria-label="Resumo por status">${cards}</section>
    <div class="dashboard-grid"><section class="panel"><div class="panel-head"><div><h2>Últimos chamados</h2><p class="subtitle">Os atendimentos abertos mais recentemente</p></div><a class="text-link" href="#chamados">Ver todos ${icon('arrow')}</a></div>${recent.content.length ? ticketTable(recent.content, true) : empty('Seu primeiro chamado começa aqui', 'Cadastre um cliente e uma categoria para abrir uma solicitação.', newButton('chamados'))}</section>
    <aside class="dashboard-aside"><section class="panel"><div class="panel-head"><h2>Acesso rápido</h2></div><div class="panel-body">${Object.entries(catalog).map(([type, config]) => `<a class="shortcut" href="#${type}"><span class="shortcut-icon">${icon(config.icon)}</span><span><strong>${config.title}</strong><small>${cache[type].length} ${type === 'categorias' ? 'cadastradas' : 'cadastrados'}</small></span>${icon('arrow')}</a>`).join('')}</div></section>
    <section class="panel"><div class="panel-head"><h2>Distribuição dos chamados</h2></div><div class="panel-body"><span class="count">${total} ${total === 1 ? 'chamado no total' : 'chamados no total'}</span>${statuses.map((s, i) => `<div class="progress-row"><span>${labels[s]}</span><strong>${total ? Math.round(counts[i].totalElements / total * 100) : 0}%</strong></div><div class="progress-track"><span class="${['', 'blue', 'amber', 'gray'][i]}" style="width:${total ? counts[i].totalElements / total * 100 : 0}%"></span></div>`).join('')}</div></section></aside></div>`;
}
function catalogTable(type, items) {
  const config = catalog[type];
  if (!items.length) return empty('Nenhum registro encontrado', 'Cadastre um registro ou tente outro termo de busca.', '', config.icon);
  return `<div class="table-wrap"><table><thead><tr><th>Nome</th>${type === 'categorias' ? '<th>Descrição</th>' : '<th>E-mail</th>'}<th>${type === 'clientes' ? 'Telefone' : type === 'tecnicos' ? 'Perfil' : 'ID'}</th><th>Ações</th></tr></thead><tbody>${items.map(item => `<tr><td><span class="cell-title">${esc(item.nome)}</span><span class="cell-sub">#${String(item.id).padStart(3, '0')}</span></td><td class="wrap">${esc(type === 'categorias' ? item.descricao || '—' : item.email)}</td><td>${type === 'tecnicos' ? badge(item.perfil) : esc(type === 'clientes' ? item.telefone || '—' : `#${item.id}`)}</td><td><div class="row-actions">${type === 'clientes' ? `<a class="btn small" href="#chamados?clienteId=${item.id}">Ver chamados</a>` : ''}<button class="icon-btn" data-edit="${type}" data-id="${item.id}" aria-label="Editar ${esc(item.nome)}">${icon('edit')}</button><button class="icon-btn delete" data-delete="${type}" data-id="${item.id}" aria-label="Excluir ${esc(item.nome)}">${icon('trash')}</button></div></td></tr>`).join('')}</tbody></table></div>`;
}
async function catalogPage(type, version) {
  const items = await api(`/${type}`);
  if (version !== renderVersion) return;
  cache[type] = items;
  const config = catalog[type];
  main.innerHTML = heading(config.title, config.description, newButton(type)) + `<section class="panel"><div class="toolbar"><label class="search">${icon('search')}<input id="catalog-search" type="search" placeholder="Buscar ${type === 'categorias' ? 'categoria' : config.singular}..." aria-label="Buscar ${config.singular}"></label><span id="catalog-count" class="count">${items.length} registros</span></div><div id="catalog-results">${catalogTable(type, items)}</div></section>`;
  document.querySelector('#catalog-search').addEventListener('input', event => {
    const search = event.target.value.trim().toLocaleLowerCase('pt-BR');
    const filtered = items.filter(item => [item.nome, item.email, item.descricao, item.telefone].some(value => String(value || '').toLocaleLowerCase('pt-BR').includes(search)));
    document.querySelector('#catalog-results').innerHTML = catalogTable(type, filtered);
    document.querySelector('#catalog-count').textContent = `${filtered.length} registros`;
  });
}
async function ticketsPage(params, version) {
  await loadCatalogs();
  const query = new URLSearchParams();
  for (const key of ['status', 'prioridade', 'clienteId', 'page']) if (params.get(key)) query.set(key, params.get(key));
  query.set('size', '10');
  query.set('sort', params.get('sort') || 'dataAbertura,desc');
  // O filtro por cliente também utiliza a rota específica prevista no backend.
  let endpoint = '/chamados';
  if (query.get('clienteId') && !query.get('status') && !query.get('prioridade')) {
    endpoint = `/clientes/${encodeURIComponent(query.get('clienteId'))}/chamados`;
    query.delete('clienteId');
  }
  const page = await api(`${endpoint}?${query}`);
  if (version !== renderVersion) return;
  const selectedClient = params.get('clienteId');
  main.innerHTML = heading('Chamados', 'Acompanhe as solicitações e mantenha o atendimento em dia.', newButton('chamados')) +
    `<section class="panel"><form class="filters" id="ticket-filters"><label>Status<select name="status">${options(statuses, params.get('status'), 'Todos os status')}</select></label><label>Prioridade<select name="prioridade">${options(priorities, params.get('prioridade'), 'Todas as prioridades')}</select></label><label>Cliente<select name="clienteId">${options(cache.clientes, selectedClient, 'Todos os clientes')}</select></label><label>Ordenar por<select name="sort"><option value="dataAbertura,desc" ${params.get('sort') !== 'dataAbertura,asc' ? 'selected' : ''}>Mais recentes</option><option value="dataAbertura,asc" ${params.get('sort') === 'dataAbertura,asc' ? 'selected' : ''}>Mais antigos</option></select></label><button class="btn primary" type="submit">Filtrar</button><a class="btn" href="#chamados">Limpar</a></form><div class="result-heading">${page.totalElements} ${page.totalElements === 1 ? 'chamado encontrado' : 'chamados encontrados'}</div>${page.content.length ? ticketTable(page.content) : empty('Nenhum chamado por aqui', 'Abra uma solicitação ou ajuste os filtros para encontrar um atendimento.')}
    <div class="pagination"><span>Página ${page.number + 1} de ${Math.max(1, page.totalPages)}</span><div class="row-actions"><button class="btn small" data-page="${page.number - 1}" ${page.first ? 'disabled' : ''}>${icon('back')}Anterior</button><button class="btn small" data-page="${page.number + 1}" ${page.last ? 'disabled' : ''}>Próxima${icon('arrow')}</button></div></div></section>`;
  document.querySelector('#ticket-filters').addEventListener('submit', event => {
    event.preventDefault();
    const values = new URLSearchParams();
    for (const [key, value] of new FormData(event.currentTarget)) if (value) values.set(key, value);
    navigate(`#chamados?${values}`);
  });
}
async function ticketDetail(id, version) {
  await loadCatalogs();
  const c = await api(`/chamados/${encodeURIComponent(id)}`);
  if (version !== renderVersion) return;
  const closed = c.status === 'FECHADO';
  main.innerHTML = `<a class="back-link" href="#chamados">${icon('back')}Voltar aos chamados</a>` + heading(`Chamado #${String(c.id).padStart(3, '0')}`, `Aberto em ${date(c.dataAbertura, true)}`, closed ? '' : `<div class="row-actions"><button class="btn" data-edit="chamados" data-id="${c.id}">${icon('edit')}Editar</button><button class="icon-btn delete" data-delete="chamados" data-id="${c.id}" aria-label="Excluir chamado">${icon('trash')}</button></div>`) +
    `${closed ? '<div class="alert info">Este chamado foi fechado. Os dados e comentários estão disponíveis para consulta.</div>' : ''}<div id="detail-error"></div><div class="detail-grid"><div><section class="panel"><div class="panel-body"><div class="detail-meta">${badge(c.status)}${badge(c.prioridade)}</div><h2>${esc(c.titulo)}</h2><p class="detail-description">${esc(c.descricao)}</p></div></section>
    <section class="panel"><div class="panel-head"><h2>Comentários <span class="count">(${c.comentarios.length})</span></h2>${icon('message')}</div><div class="panel-body">${c.comentarios.length ? c.comentarios.map(comment => `<article class="comment"><span class="avatar">${esc(comment.autor.charAt(0).toUpperCase())}</span><div class="comment-content"><div class="comment-header"><strong>${esc(comment.autor)}</strong><small>${date(comment.data, true)}</small></div><p>${esc(comment.texto)}</p></div></article>`).join('') : '<p class="subtitle">Nenhum comentário ainda.</p>'}${closed ? '' : `<form id="comment-form" class="comment-form"><div class="form-error"></div><label class="field">Seu nome<input name="autor" required maxlength="100" placeholder="Quem está registrando o comentário?"></label><label class="field">Comentário<textarea name="texto" required maxlength="2000" placeholder="Registre uma atualização sobre o atendimento..."></textarea></label><button class="btn primary" type="submit">${icon('message')}Adicionar comentário</button></form>`}</div></section></div>
    <aside><section class="panel"><div class="panel-head"><h2>Informações do chamado</h2></div><div class="panel-body"><dl class="detail-info"><div><dt>Cliente</dt><dd>${esc(nameOf('clientes', c.clienteId))}</dd></div><div><dt>Categoria</dt><dd>${esc(nameOf('categorias', c.categoriaId))}</dd></div><div><dt>Técnico</dt><dd>${esc(nameOf('tecnicos', c.tecnicoId))}</dd></div><div><dt>Prioridade</dt><dd>${esc(labels[c.prioridade])}</dd></div>${closed ? `<div><dt>Fechamento</dt><dd>${date(c.dataFechamento, true)}</dd></div>` : ''}</dl>${closed ? '' : `<form class="inline-form" id="assign-form"><div class="form-error"></div><label class="field">Responsável pelo atendimento<select name="tecnicoId" required>${options(cache.tecnicos, c.tecnicoId, 'Selecione um técnico')}</select></label>${!cache.tecnicos.length ? '<p class="subtitle">Cadastre um técnico para atribuir o atendimento.</p>' : ''}<button class="btn" type="submit" ${!cache.tecnicos.length ? 'disabled' : ''}>Atribuir técnico</button></form><form class="inline-form" id="status-form"><div class="form-error"></div><label class="field">Status do chamado<select name="status" required>${options(statuses, c.status)}</select></label><p class="subtitle" style="margin-bottom:14px">Para iniciar o atendimento, atribua um técnico. Após o fechamento, o chamado não poderá ser alterado.</p><button class="btn primary" type="submit">Atualizar status</button></form>`}</div></section></aside></div>`;
  if (!closed) {
    bindActionForm('#comment-form', data => api(`/chamados/${c.id}/comentarios`, 'POST', data), 'Comentário adicionado.');
    bindActionForm('#assign-form', data => api(`/chamados/${c.id}/atribuir`, 'PATCH', { tecnicoId: Number(data.tecnicoId) }), 'Técnico atribuído.');
    bindActionForm('#status-form', async data => {
      if (data.status === 'FECHADO') {
        const confirmed = await confirmClose();
        if (!confirmed) return false;
      }
      return api(`/chamados/${c.id}/status`, 'PATCH', data);
    }, 'Status atualizado.');
  }
}
function bindActionForm(selector, action, success) {
  document.querySelector(selector).addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    form.querySelector('.form-error').innerHTML = '';
    try {
      const result = await action(Object.fromEntries(new FormData(form)));
      if (result === false) return;
      showToast(success);
      await render();
    } catch (error) { form.querySelector('.form-error').innerHTML = errorHtml(error); }
    finally { button.disabled = false; }
  });
}
function openModal(title, content, footer) {
  document.querySelector('#modal-body').innerHTML = `<div class="modal-head"><h2 id="modal-title">${esc(title)}</h2><button class="icon-btn" data-close aria-label="Fechar">${icon('close')}</button></div>${content}${footer || ''}`;
  if (!modal.open) modal.showModal();
}
function confirmClose() {
  return new Promise(resolve => {
    openModal('Fechar chamado?', '<div class="form-body"><p>Após o fechamento, não será possível editar o chamado, alterar o responsável ou adicionar comentários.</p></div>', '<div class="modal-footer"><button class="btn" data-close>Cancelar</button><button class="btn primary" id="confirm-close">Fechar chamado</button></div>');
    const cancel = () => resolve(false);
    modal.addEventListener('close', cancel, { once: true });
    document.querySelector('#confirm-close').onclick = () => {
      modal.removeEventListener('close', cancel);
      modal.close();
      resolve(true);
    };
  });
}
function fieldHtml(field, item) {
  const [key, label, type, limit, required] = field;
  const value = item[key] || '';
  const attributes = `name="${key}" ${required ? 'required' : ''}`;
  let input;
  if (type === 'select') input = `<select ${attributes}>${options(limit, value)}</select>`;
  else if (type === 'textarea') input = `<textarea ${attributes} maxlength="${limit}">${esc(value)}</textarea>`;
  else input = `<input ${attributes} type="${type}" maxlength="${limit}" value="${esc(value)}" ${key === 'email' ? 'autocomplete="email"' : ''}>`;
  return `<label class="field">${label}${required ? ' *' : ' (opcional)'}${input}</label>`;
}
async function editForm(type, id) {
  try {
    await loadCatalogs();
    const item = id ? await api(`/${type}/${id}`) : {};
    let fields;
    let title;
    if (type === 'chamados') {
      if (item.status === 'FECHADO') { showToast('Chamados fechados estão disponíveis apenas para consulta.'); return; }
      title = id ? 'Editar chamado' : 'Novo chamado';
      if (!cache.clientes.length || !cache.categorias.length) {
        openModal(title, '<div class="form-body"><div class="alert info">Para abrir um chamado, cadastre pelo menos um cliente e uma categoria.</div><div class="row-actions"><a class="btn" href="#clientes" data-close>Ir para clientes</a><a class="btn" href="#categorias" data-close>Ir para categorias</a></div></div>', '<div class="modal-footer"><button class="btn" data-close>Voltar</button></div>');
        return;
      }
      fields = fieldHtml(['titulo', 'Título', 'text', 150, true], item) + fieldHtml(['descricao', 'Descrição', 'textarea', 2000, true], item) +
        `<div class="form-row"><label class="field">Cliente *<select name="clienteId" required ${id ? 'disabled' : ''}>${options(cache.clientes, item.clienteId)}</select>${id ? '<small>O cliente do chamado não pode ser alterado.</small>' : ''}</label><label class="field">Categoria *<select name="categoriaId" required>${options(cache.categorias, item.categoriaId)}</select></label></div><label class="field">Prioridade *<select name="prioridade" required>${options(priorities, item.prioridade || 'MEDIA')}</select></label>`;
    } else {
      const config = catalog[type];
      title = id ? `Editar ${config.singular}` : `${type === 'categorias' ? 'Nova' : 'Novo'} ${config.singular}`;
      fields = config.fields.map(field => fieldHtml(field, item)).join('');
    }
    openModal(title, `<form id="edit-form"><div class="form-body"><div class="form-error"></div>${fields}<span class="count">* Campos obrigatórios</span></div><div class="modal-footer"><button class="btn" type="button" data-close>Cancelar</button><button class="btn primary" type="submit">${id ? 'Salvar alterações' : type === 'chamados' ? 'Abrir chamado' : 'Cadastrar'}</button></div></form>`);
    document.querySelector('#edit-form').addEventListener('submit', async event => {
      event.preventDefault();
      const form = event.currentTarget;
      const data = Object.fromEntries(new FormData(form));
      Object.keys(data).forEach(key => { if (typeof data[key] === 'string') data[key] = data[key].trim(); });
      if (type === 'chamados') {
        data.clienteId = id ? item.clienteId : Number(data.clienteId);
        data.categoriaId = Number(data.categoriaId);
      }
      const button = form.querySelector('button[type="submit"]');
      button.disabled = true;
      modalBusy = true;
      form.querySelector('.form-error').innerHTML = '';
      try {
        const saved = await api(`/${type}${id ? `/${id}` : ''}`, id ? 'PUT' : 'POST', data);
        modal.close();
        showToast(id ? 'Alterações salvas.' : 'Cadastro realizado.');
        if (type === 'chamados' && !id) navigate(`#chamados/${saved.id}`); else await render();
      } catch (error) { form.querySelector('.form-error').innerHTML = errorHtml(error); }
      finally { modalBusy = false; button.disabled = false; }
    });
  } catch (error) { openModal('Não foi possível abrir o formulário', `<div class="form-body">${errorHtml(error)}</div>`, '<div class="modal-footer"><button class="btn" data-close>Voltar</button></div>'); }
}
async function deleteRecord(type, id) {
  try {
    const item = await api(`/${type}/${id}`);
    if (type === 'chamados' && item.status === 'FECHADO') { showToast('Não é permitido excluir um chamado fechado.'); return; }
    openModal('Excluir registro?', `<div class="form-body"><div class="form-error"></div><p>Deseja excluir <strong>${esc(item.nome || item.titulo)}</strong>?</p><p class="subtitle">Esta ação não pode ser desfeita.${type === 'chamados' ? ' Os comentários vinculados também serão excluídos.' : ' Cadastros vinculados a chamados podem não permitir exclusão.'}</p></div>`, '<div class="modal-footer"><button class="btn" data-close>Cancelar</button><button class="btn danger" id="confirm-delete">Excluir registro</button></div>');
    document.querySelector('#confirm-delete').onclick = async event => {
      const button = event.currentTarget;
      button.disabled = true;
      modalBusy = true;
      try {
        await api(`/${type}/${id}`, 'DELETE');
        modal.close();
        showToast('Registro excluído.');
        if (type === 'chamados') navigate('#chamados'); else await render();
      } catch (error) { modal.querySelector('.form-error').innerHTML = errorHtml(error); }
      finally { modalBusy = false; button.disabled = false; }
    };
  } catch (error) { showToast(error.message); }
}
async function render() {
  const version = ++renderVersion;
  const [path, query = ''] = (location.hash.slice(1) || 'dashboard').split('?');
  const [section, id] = path.split('/');
  const titles = { dashboard: 'Dashboard', chamados: 'Chamados', clientes: 'Clientes', categorias: 'Categorias', tecnicos: 'Técnicos' };
  document.querySelector('#breadcrumb').textContent = titles[section] || 'Página não encontrada';
  document.title = `${titles[section] || 'Página não encontrada'} · Chamados`;
  document.querySelectorAll('[data-nav]').forEach(link => {
    const active = link.dataset.nav === section;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page'); else link.removeAttribute('aria-current');
  });
  main.innerHTML = '<div class="loading" role="status"><span class="spinner"></span>Carregando informações...</div>';
  main.setAttribute('aria-busy', 'true');
  try {
    if (section === 'dashboard') await dashboard(version);
    else if (section === 'chamados' && id) await ticketDetail(id, version);
    else if (section === 'chamados') await ticketsPage(new URLSearchParams(query), version);
    else if (catalog[section]) await catalogPage(section, version);
    else main.innerHTML = empty('Página não encontrada', 'Escolha uma das opções do menu para continuar.', '<a class="btn primary" href="#dashboard">Ir para o dashboard</a>');
  } catch (error) {
    if (version === renderVersion) main.innerHTML = heading('Não foi possível carregar', 'Verifique a conexão e tente novamente.') + errorHtml(error) + '<button class="btn" data-retry>Tentar novamente</button>';
  } finally { if (version === renderVersion) main.removeAttribute('aria-busy'); }
}
document.addEventListener('click', event => {
  const target = event.target.closest('button, a');
  if (!target) return;
  if (target.hasAttribute('data-close') && !modalBusy) modal.close();
  if (target.dataset.new) editForm(target.dataset.new);
  if (target.dataset.edit) editForm(target.dataset.edit, Number(target.dataset.id));
  if (target.dataset.delete) deleteRecord(target.dataset.delete, Number(target.dataset.id));
  if (target.hasAttribute('data-retry')) render();
  if (target.hasAttribute('data-page') && !target.disabled) {
    const query = new URLSearchParams(location.hash.split('?')[1] || '');
    query.set('page', target.dataset.page);
    navigate(`#chamados?${query}`);
  }
});
modal.addEventListener('cancel', event => { if (modalBusy) event.preventDefault(); });
window.addEventListener('hashchange', () => { if (modal.open && !modalBusy) modal.close(); render(); });
document.querySelectorAll('[data-icon]').forEach(element => { element.innerHTML = icon(element.dataset.icon); });
render();
