---
title: FundManager V2
layout: default
---

<style>
#search-box {
  width: 100%;
  padding: 0.75em 1em;
  border: 1px solid #30363d;
  border-radius: 6px;
  background: #0d1117;
  color: #c9d1d9;
  font-size: 1em;
  margin: 1em 0 0.5em;
  outline: none;
}
#search-box:focus { border-color: #58a6ff; }
#result-count { color: #8b949e; font-size: 0.85em; margin-bottom: 1em; }
.post-card {
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 1em 1.25em;
  margin-bottom: 0.75em;
  background: #161b22;
  transition: border-color 0.15s;
}
.post-card:hover { border-color: #58a6ff; }
.post-card h3 { margin: 0 0 0.3em; border: none; font-size: 1.1em; }
.post-card h3 a { color: #58a6ff; }
.post-card .meta { color: #8b949e; font-size: 0.8em; }
.post-card .desc { color: #c9d1d9; font-size: 0.9em; margin-top: 0.3em; }
.post-tag {
  display: inline-block;
  background: rgba(88,166,255,0.15);
  color: #58a6ff;
  padding: 0.1em 0.5em;
  border-radius: 12px;
  font-size: 0.75em;
  margin-right: 0.3em;
}
.pagination {
  display: flex;
  justify-content: center;
  gap: 0.5em;
  margin-top: 2em;
  flex-wrap: wrap;
}
.pagination button {
  padding: 0.4em 0.8em;
  border: 1px solid #30363d;
  border-radius: 4px;
  background: #161b22;
  color: #c9d1d9;
  cursor: pointer;
  font-size: 0.85em;
}
.pagination button.active { background: #58a6ff; color: #000; border-color: #58a6ff; }
.pagination button:disabled { opacity: 0.4; cursor: default; }
</style>

<input type="text" id="search-box" placeholder="Search posts... (e.g. workflow, android, api)" autofocus>
<div id="result-count"></div>
<div id="post-list"></div>
<div id="pagination" class="pagination"></div>

<script>
// ponytail: client-side search + pagination, add lunr.js when >100 posts
const POSTS = [
  {slug:"dashboard",title:"Technical Dashboard",desc:"Project overview: 7-stage workflow, 6 roles, stack, metrics, hard constraints.",tags:["dashboard","metrics","overview"]},
  {slug:"product",title:"Product Requirements",desc:"Problem statement, 6 user personas, 7 core capabilities including offline-first and RBAC.",tags:["product","prd","personas"]},
  {slug:"architecture",title:"System Architecture",desc:"Client-server design with data flow diagrams, security model, infrastructure at 103.94.11.78.",tags:["architecture","system-design","infrastructure"]},
  {slug:"backend",title:"Backend — Laravel 11 API",desc:"22 API endpoints, 700-line TaskExpenseController, authorization matrix, pagu enforcement logic.",tags:["backend","api","laravel"]},
  {slug:"android",title:"Android — Kotlin/Compose",desc:"13 Compose screens, Room DB entities, sync engine with outbox pattern, multi-user per device.",tags:["android","compose","room"]},
  {slug:"database",title:"Database Schema — PostgreSQL 14",desc:"31 tables with full relationships diagram, 4-layer nominal tracking, additive migrations.",tags:["database","schema","postgresql"]},
  {slug:"workflows",title:"Budget Request Workflow — Full Lifecycle",desc:"Complete 7-stage lifecycle with server-side transition pseudocode, audit trail, edge cases.",tags:["workflow","budget","approval"]},
  {slug:"sessions",title:"Session Logs",desc:"Development history: major build session (21 commits), post-gap fixes (12 commits), blog creation.",tags:["sessions","history","development"]},
  {slug:"open-qna",title:"Open Q&A — 50 Questions",desc:"Technical FAQ covering product, architecture, finance, roles, web, budget, authorization, database.",tags:["faq","qna","knowledge-base"]},
  {slug:"resources",title:"Resources",desc:"Hermes 18 skill categories, Android & Laravel dependencies, config files, external references.",tags:["resources","skills","dependencies"]},
  {slug:"hermes",title:"HERMES.md — AI Context",desc:"AI agent context injection: stack, vault structure, hard constraints, workflow conventions.",tags:["hermes","ai","context"]},
  {slug:"soul",title:"SOUL.md — Identity & Operating System",desc:"Hermes operating system: 9 sections covering strategic thinking, godly coding, anti-hallucination.",tags:["soul","identity","rules"]},
  {slug:"android-architecture-best-practices",title:"Android Architecture Best Practices",desc:"Official Google guidance on offline-first architecture, UDF, testing strategy, dependency injection.",tags:["android","architecture","best-practice"]},
  {slug:"android-compose-ui-pattern",title:"Compose UI Pattern",desc:"ViewModel + UiState + Screen pattern with real FundManager code examples and navigation.",tags:["android","compose","pattern"]}
];

const PER_PAGE = 6;
let filtered = POSTS;
let page = 0;

function render() {
  const q = document.getElementById('search-box').value.toLowerCase();
  filtered = q ? POSTS.filter(p =>
    p.title.toLowerCase().includes(q) ||
    p.desc.toLowerCase().includes(q) ||
    p.tags.some(t => t.includes(q))
  ) : POSTS;

  document.getElementById('result-count').textContent =
    q ? `${filtered.length} post${filtered.length !== 1 ? 's' : ''} found` : '';

  const totalPages = Math.ceil(filtered.length / PER_PAGE);
  if (page >= totalPages) page = 0;
  const slice = filtered.slice(page * PER_PAGE, (page + 1) * PER_PAGE);

  document.getElementById('post-list').innerHTML = slice.map(p =>
    `<div class="post-card">
      <h3><a href="blog/${p.slug}">${p.title}</a></h3>
      <div class="meta">${p.tags.map(t => `<span class="post-tag">${t}</span>`).join(' ')}</div>
      <div class="desc">${p.desc}</div>
    </div>`
  ).join('');

  // pagination
  const pg = document.getElementById('pagination');
  pg.innerHTML = '';
  if (totalPages <= 1) return;
  for (let i = 0; i < totalPages; i++) {
    const btn = document.createElement('button');
    btn.textContent = i + 1;
    if (i === page) btn.className = 'active';
    btn.onclick = () => { page = i; render(); window.scrollTo(0,0); };
    pg.appendChild(btn);
  }
}

document.getElementById('search-box').addEventListener('input', () => { page = 0; render(); });
render();
</script>

---

FundManager V2: Budget management for field engineering — Kotlin/Compose + Laravel 11 + PostgreSQL.

[GitHub](https://github.com/mldmustamin/Project-Budgeting) · [ACTION_LOG](https://github.com/mldmustamin/Project-Budgeting/blob/main/ACTION_LOG.md) · [PRD](https://github.com/mldmustamin/Project-Budgeting/blob/main/PRD.md)
