const FALLBACK_CATEGORIES = [
    "tech", "ai", "gadgets", "internet",
    "geopolitics", "global-market", "conflicts", "diplomacy",
    "programming", "web", "mobile", "devops",
    "games", "console", "pc", "mobile-gaming",
    "trending", "world-news", "entertainment", "lifestyle"
];

const state = {
    apiBase: "/api",
    articles: [],
    authors: [],
    categories: [],
    page: 0,
    size: 20,
    totalPages: 1,
    adminMode: false
};

const el = {};

document.addEventListener("DOMContentLoaded", () => {
    bindElements();
    bindEvents();
    initialize();
});

function bindElements() {
    el.apiForm = document.getElementById("api-config-form");
    el.apiBase = document.getElementById("api-base");
    el.apiStatus = document.getElementById("api-status");

    el.statTotal = document.getElementById("stat-total");
    el.statPublished = document.getElementById("stat-published");
    el.statDrafts = document.getElementById("stat-drafts");
    el.statViews = document.getElementById("stat-views");

    el.searchInput = document.getElementById("search-input");
    el.statusFilter = document.getElementById("status-filter");
    el.newsList = document.getElementById("news-list");
    el.adminModeAlert = document.getElementById("admin-mode-alert");

    el.prevPage = document.getElementById("prev-page");
    el.nextPage = document.getElementById("next-page");
    el.pageLabel = document.getElementById("page-label");

    el.refreshList = document.getElementById("refresh-list");
    el.resetForm = document.getElementById("reset-form");
    el.toast = document.getElementById("toast");

    el.authorForm = document.getElementById("author-form");
    el.aiTopic = document.getElementById("ai-topic");
    el.aiAngle = document.getElementById("ai-angle");
    el.aiTone = document.getElementById("ai-tone");
    el.aiGenerate = document.getElementById("ai-generate");

    el.articleForm = document.getElementById("article-form");
    el.articleId = document.getElementById("article-id");
    el.title = document.getElementById("title");
    el.subtitle = document.getElementById("subtitle");
    el.excerpt = document.getElementById("excerpt");
    el.content = document.getElementById("content");
    el.category = document.getElementById("category");
    el.authorId = document.getElementById("author-id");
    el.tags = document.getElementById("tags");
    el.imageUrl = document.getElementById("image-url");
    el.seoTitle = document.getElementById("seo-title");
    el.seoDescription = document.getElementById("seo-description");
    el.seoImage = document.getElementById("seo-image");
    el.featured = document.getElementById("featured");
    el.isDraft = document.getElementById("is-draft");

    el.saveDraft = document.getElementById("save-draft");
    el.publishNow = document.getElementById("publish-now");
}

function bindEvents() {
    el.apiForm.addEventListener("submit", onApiConnect);
    el.refreshList.addEventListener("click", () => refreshPageData());

    el.searchInput.addEventListener("input", renderArticleList);
    el.statusFilter.addEventListener("change", renderArticleList);

    el.prevPage.addEventListener("click", () => loadArticles(state.page - 1));
    el.nextPage.addEventListener("click", () => loadArticles(state.page + 1));

    el.newsList.addEventListener("click", onArticleAction);

    el.resetForm.addEventListener("click", resetArticleForm);
    el.saveDraft.addEventListener("click", () => {
        el.isDraft.checked = true;
        el.articleForm.requestSubmit();
    });
    el.publishNow.addEventListener("click", () => {
        el.isDraft.checked = false;
        el.articleForm.requestSubmit();
    });

    el.articleForm.addEventListener("submit", onArticleSubmit);
    el.authorForm.addEventListener("submit", onAuthorSubmit);
    el.aiGenerate.addEventListener("click", onGenerateDraftBase);
}

async function initialize() {
    el.apiBase.value = state.apiBase;
    const detectedBase = await detectApiBase();
    if (!detectedBase) {
        setApiStatus("API nao encontrada. Ajuste a base e clique em Conectar.", true);
        return;
    }

    await bootstrapData();
}

async function onApiConnect(event) {
    event.preventDefault();
    const manualBase = normalizeBase(el.apiBase.value.trim() || "/api");
    state.apiBase = manualBase;

    const isAlive = await testApiBase(state.apiBase);
    if (!isAlive) {
        setApiStatus("Falha de conexao com a API nesta base.", true);
        return;
    }

    setApiStatus("Conectado com sucesso.", false);
    localStorage.setItem("newsportal_api_base", state.apiBase);
    await bootstrapData();
}

async function bootstrapData() {
    try {
        await Promise.all([
            loadAuthors(),
            loadCategories(),
            loadStats(),
            loadArticles(0)
        ]);
        showToast("Painel pronto para uso.");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function detectApiBase() {
    const candidateBases = unique([
        normalizeBase(localStorage.getItem("newsportal_api_base") || ""),
        "/api",
        ""
    ]).filter((base) => base !== null);

    for (const base of candidateBases) {
        if (await testApiBase(base)) {
            state.apiBase = base;
            el.apiBase.value = state.apiBase;
            localStorage.setItem("newsportal_api_base", state.apiBase);
            setApiStatus(`API conectada em ${state.apiBase || "/"}`, false);
            return state.apiBase;
        }
    }

    return null;
}

async function testApiBase(base) {
    try {
        const response = await fetch(buildUrl(base, "/articles?page=0&size=1"));
        return response.ok;
    } catch (_) {
        return false;
    }
}

function setApiStatus(message, isError) {
    el.apiStatus.textContent = message;
    el.apiStatus.style.color = isError ? "#ff9c9c" : "#86dfbd";
}

async function loadStats() {
    try {
        const stats = await apiRequest("/articles/stats");
        el.statTotal.textContent = stats.totalArticles ?? "-";
        el.statPublished.textContent = stats.publishedArticles ?? "-";
        el.statDrafts.textContent = stats.draftArticles ?? "-";
        el.statViews.textContent = stats.recentViews ?? "-";
    } catch (_) {
        el.statTotal.textContent = "-";
        el.statPublished.textContent = "-";
        el.statDrafts.textContent = "-";
        el.statViews.textContent = "-";
    }
}

async function loadAuthors() {
    state.authors = await apiRequest("/authors");
    renderAuthorSelect();
}

async function loadCategories() {
    try {
        const categories = await apiRequest("/articles/categories");
        state.categories = categories && categories.length ? categories : FALLBACK_CATEGORIES;
    } catch (_) {
        state.categories = FALLBACK_CATEGORIES;
    }

    renderCategorySelect();
}

async function loadArticles(page = 0) {
    state.page = Math.max(page, 0);

    let responsePage;
    try {
        responsePage = await apiRequest(`/articles/admin?page=${state.page}&size=${state.size}`);
        state.adminMode = true;
        el.adminModeAlert.classList.add("hidden");
    } catch (_) {
        responsePage = await apiRequest(`/articles?page=${state.page}&size=${state.size}`);
        state.adminMode = false;
        el.adminModeAlert.textContent = "Modo publico: endpoint /articles/admin indisponivel. Rascunhos nao aparecem na lista.";
        el.adminModeAlert.classList.remove("hidden");
    }

    state.articles = responsePage.content || [];
    state.totalPages = Math.max(1, responsePage.totalPages || 1);

    renderArticleList();
    renderPager();
}

function renderAuthorSelect() {
    const previous = el.authorId.value;
    el.authorId.innerHTML = "";

    if (!state.authors.length) {
        const emptyOption = document.createElement("option");
        emptyOption.value = "";
        emptyOption.textContent = "Cadastre um autor primeiro";
        el.authorId.appendChild(emptyOption);
        return;
    }

    state.authors.forEach((author) => {
        const option = document.createElement("option");
        option.value = author.id;
        option.textContent = `${author.name} (${author.email})`;
        el.authorId.appendChild(option);
    });

    if (previous) {
        el.authorId.value = previous;
    }
}

function renderCategorySelect() {
    const previous = el.category.value;
    el.category.innerHTML = "";

    state.categories.forEach((category) => {
        const option = document.createElement("option");
        option.value = category;
        option.textContent = prettifyCategory(category);
        el.category.appendChild(option);
    });

    if (previous) {
        el.category.value = previous;
    }
}

function renderArticleList() {
    const query = el.searchInput.value.trim().toLowerCase();
    const status = el.statusFilter.value;

    const filtered = state.articles.filter((article) => {
        if (status === "draft" && !article.isDraft) {
            return false;
        }
        if (status === "published" && article.isDraft) {
            return false;
        }

        if (!query) {
            return true;
        }

        const haystack = [
            article.title,
            article.excerpt,
            article.category,
            (article.tags || []).join(" "),
            article.author?.name
        ].join(" ").toLowerCase();

        return haystack.includes(query);
    });

    if (!filtered.length) {
        el.newsList.innerHTML = "<p class='muted'>Nenhum artigo encontrado nesta pagina.</p>";
        return;
    }

    el.newsList.innerHTML = filtered.map((article) => {
        const category = prettifyCategory(article.category || "");
        const statusBadge = article.isDraft
            ? "<span class='badge draft'>Rascunho</span>"
            : "<span class='badge published'>Publicado</span>";

        const tags = (article.tags || []).slice(0, 3).map((tag) => `<span class='badge'>#${escapeHtml(tag)}</span>`).join("");

        return `
            <article class="news-card">
                <h3 class="news-title">${escapeHtml(article.title || "Sem titulo")}</h3>
                <div class="news-meta">
                    ${statusBadge}
                    <span class="badge">${escapeHtml(category)}</span>
                    ${tags}
                </div>
                <p class="muted">${escapeHtml(truncate(article.excerpt || "Sem resumo", 170))}</p>
                <div class="news-actions">
                    <button class="ghost" data-action="edit" data-id="${article.id}">Editar</button>
                    ${article.isDraft ? `<button class="warning" data-action="approve" data-id="${article.id}">Aprovar</button>` : ""}
                    <button class="danger" data-action="delete" data-id="${article.id}">Excluir</button>
                </div>
            </article>
        `;
    }).join("");
}

function renderPager() {
    el.pageLabel.textContent = `Pagina ${state.page + 1} de ${state.totalPages}`;
    el.prevPage.disabled = state.page === 0;
    el.nextPage.disabled = state.page >= state.totalPages - 1;
}

async function onArticleAction(event) {
    const actionButton = event.target.closest("button[data-action]");
    if (!actionButton) {
        return;
    }

    const id = Number(actionButton.dataset.id);
    const article = state.articles.find((item) => item.id === id);
    if (!article) {
        showToast("Artigo nao encontrado na lista atual.", true);
        return;
    }

    const action = actionButton.dataset.action;

    if (action === "edit") {
        fillForm(article);
        return;
    }

    if (action === "approve") {
        await approveDraft(article);
        return;
    }

    if (action === "delete") {
        await deleteArticle(id);
    }
}

async function approveDraft(article) {
    if (!article.author?.id) {
        showToast("Nao foi possivel aprovar: artigo sem autor valido.", true);
        return;
    }

    const payload = articleToRequestPayload(article, false);

    try {
        await apiRequest(`/articles/${article.id}`, {
            method: "PUT",
            body: JSON.stringify(payload)
        });

        showToast("Rascunho aprovado e publicado.");
        await refreshPageData();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function deleteArticle(id) {
    const confirmed = window.confirm("Deseja realmente excluir este artigo?");
    if (!confirmed) {
        return;
    }

    try {
        await apiRequest(`/articles/${id}`, { method: "DELETE" });
        showToast("Artigo excluido.");
        await refreshPageData();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onArticleSubmit(event) {
    event.preventDefault();

    if (!state.authors.length) {
        showToast("Crie um autor antes de salvar artigo.", true);
        return;
    }

    const payload = getArticlePayloadFromForm();
    const id = Number(el.articleId.value);
    const isEdit = Number.isFinite(id) && id > 0;

    try {
        const response = await apiRequest(isEdit ? `/articles/${id}` : "/articles", {
            method: isEdit ? "PUT" : "POST",
            body: JSON.stringify(payload)
        });

        showToast(isEdit ? "Artigo atualizado." : "Artigo criado.");
        fillForm(response);
        await refreshPageData();
    } catch (error) {
        showToast(error.message, true);
    }
}

async function onAuthorSubmit(event) {
    event.preventDefault();

    const payload = {
        name: document.getElementById("author-name").value.trim(),
        email: document.getElementById("author-email").value.trim(),
        bio: document.getElementById("author-bio").value.trim(),
        avatarUrl: document.getElementById("author-avatar").value.trim()
    };

    try {
        const author = await apiRequest("/authors", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        showToast("Autor criado com sucesso.");
        el.authorForm.reset();
        await loadAuthors();
        el.authorId.value = String(author.id);
    } catch (error) {
        showToast(error.message, true);
    }
}

function onGenerateDraftBase() {
    const topic = el.aiTopic.value.trim();
    const angle = el.aiAngle.value.trim() || "impactos e desdobramentos";
    const tone = el.aiTone.value;

    if (!topic) {
        showToast("Informe uma pauta principal para gerar o rascunho.", true);
        return;
    }

    const title = `${capitalize(topic)}: ${angle}`;
    const excerpt = `Entenda ${topic.toLowerCase()} com foco em ${angle.toLowerCase()}. Veja contexto, riscos, oportunidades e o que observar nos proximos meses.`;

    const styleLine = tone === "opinativo"
        ? "A analise abaixo apresenta contexto e um ponto de vista argumentado."
        : tone === "didatico"
            ? "A analise abaixo explica os conceitos de forma direta e acessivel."
            : "A analise abaixo organiza fatos, dados e cenarios para leitura estrategica.";

    const draft = [
        `Introducao`,
        `Nos ultimos meses, ${topic.toLowerCase()} ganhou relevancia no debate publico e no mercado. ${styleLine}`,
        ``,
        `Panorama atual`,
        `O tema se conecta a mudancas regulatorias, comportamento do consumidor e aceleracao tecnologica. Para evitar conclusoes apressadas, vale comparar o movimento local com tendencias globais.`,
        ``,
        `Principais impactos`,
        `1. Empresas e profissionais precisam ajustar estrategia e qualificacao.`,
        `2. Consumidores percebem mudancas de preco, servico e experiencia.`,
        `3. O ecossistema de inovacao responde com novos produtos e parcerias.`,
        ``,
        `Riscos e oportunidades`,
        `Entre os riscos estao execucao apressada, baixa governanca e excesso de expectativa. Do lado das oportunidades, surgem ganhos de produtividade, novos modelos de negocio e abertura de mercado.`,
        ``,
        `Conclusao`,
        `Para acompanhar ${topic.toLowerCase()} com criterio, o melhor caminho e combinar evidencia, contexto e acompanhamento continuo dos proximos indicadores.`
    ].join("\\n\\n");

    el.title.value = title;
    el.excerpt.value = excerpt;
    el.content.value = draft;
    el.tags.value = buildTagSuggestion(topic, angle).join(", ");
    el.seoTitle.value = title;
    el.seoDescription.value = excerpt;

    const categorySuggestion = inferCategory(topic, angle);
    if (state.categories.includes(categorySuggestion)) {
        el.category.value = categorySuggestion;
    }

    showToast("Rascunho base gerado. Agora revise e publique.");
}

function getArticlePayloadFromForm() {
    return {
        title: el.title.value.trim(),
        subtitle: el.subtitle.value.trim(),
        content: el.content.value.trim(),
        excerpt: el.excerpt.value.trim(),
        imageUrl: el.imageUrl.value.trim(),
        category: el.category.value,
        tags: splitTags(el.tags.value),
        authorId: Number(el.authorId.value),
        featured: el.featured.checked,
        isDraft: el.isDraft.checked,
        seoTitle: el.seoTitle.value.trim(),
        seoDescription: el.seoDescription.value.trim(),
        seoImage: el.seoImage.value.trim()
    };
}

function articleToRequestPayload(article, isDraft) {
    return {
        title: article.title || "",
        subtitle: article.subtitle || "",
        content: article.content || "",
        excerpt: article.excerpt || "",
        imageUrl: article.imageUrl || "",
        category: article.category || "tech",
        tags: article.tags || [],
        authorId: article.author?.id,
        featured: Boolean(article.featured),
        isDraft,
        seoTitle: article.seoTitle || "",
        seoDescription: article.seoDescription || "",
        seoImage: article.seoImage || ""
    };
}

function fillForm(article) {
    el.articleId.value = article.id || "";
    el.title.value = article.title || "";
    el.subtitle.value = article.subtitle || "";
    el.excerpt.value = article.excerpt || "";
    el.content.value = article.content || "";
    el.category.value = article.category || state.categories[0] || "tech";
    el.authorId.value = article.author?.id ? String(article.author.id) : "";
    el.tags.value = (article.tags || []).join(", ");
    el.imageUrl.value = article.imageUrl || "";
    el.seoTitle.value = article.seoTitle || "";
    el.seoDescription.value = article.seoDescription || "";
    el.seoImage.value = article.seoImage || "";
    el.featured.checked = Boolean(article.featured);
    el.isDraft.checked = Boolean(article.isDraft);

    window.scrollTo({ top: 0, behavior: "smooth" });
}

function resetArticleForm() {
    el.articleForm.reset();
    el.articleId.value = "";
    el.isDraft.checked = true;

    if (state.categories.length) {
        el.category.value = state.categories[0];
    }

    if (state.authors.length) {
        el.authorId.value = String(state.authors[0].id);
    }
}

async function refreshPageData() {
    await Promise.all([
        loadStats(),
        loadArticles(state.page)
    ]);
}

async function apiRequest(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    const response = await fetch(buildUrl(state.apiBase, path), {
        ...options,
        headers
    });

    const text = await response.text();
    const data = parseBody(text);

    if (!response.ok) {
        throw new Error(extractErrorMessage(data, response.status));
    }

    return data;
}

function parseBody(text) {
    if (!text) {
        return null;
    }

    try {
        return JSON.parse(text);
    } catch (_) {
        return text;
    }
}

function extractErrorMessage(data, status) {
    if (!data) {
        return `Erro HTTP ${status}`;
    }

    if (typeof data === "string") {
        return data;
    }

    if (data.message) {
        return data.message;
    }

    const firstValue = Object.values(data)[0];
    if (typeof firstValue === "string") {
        return firstValue;
    }

    return `Erro HTTP ${status}`;
}

function splitTags(raw) {
    if (!raw.trim()) {
        return [];
    }

    return unique(raw.split(",").map((tag) => tag.trim()).filter(Boolean));
}

function unique(list) {
    return [...new Set(list)];
}

function normalizeBase(base) {
    if (base === null || base === undefined) {
        return "/api";
    }

    if (!base.trim()) {
        return "";
    }

    const withSlash = base.startsWith("/") ? base : `/${base}`;
    return withSlash.replace(/\/+$/, "");
}

function buildUrl(base, path) {
    const normalizedBase = normalizeBase(base);
    return `${normalizedBase}${path}`;
}

function prettifyCategory(category) {
    return String(category || "")
        .replaceAll("-", " ")
        .replace(/\b\w/g, (m) => m.toUpperCase());
}

function truncate(value, maxLen) {
    if (value.length <= maxLen) {
        return value;
    }

    return `${value.slice(0, maxLen - 3)}...`;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function showToast(message, isError = false) {
    el.toast.textContent = message;
    el.toast.classList.remove("hidden");
    el.toast.style.borderColor = isError ? "#a25555" : "#4f7b98";
    el.toast.style.background = isError ? "#40232f" : "#0f2c3f";

    window.setTimeout(() => {
        el.toast.classList.add("hidden");
    }, 3500);
}

function inferCategory(topic, angle) {
    const text = `${topic} ${angle}`.toLowerCase();
    if (text.includes("ia") || text.includes("inteligencia artificial")) return "ai";
    if (text.includes("program") || text.includes("codigo")) return "programming";
    if (text.includes("mobile") || text.includes("celular")) return "mobile";
    if (text.includes("jogo") || text.includes("game")) return "games";
    if (text.includes("mercado") || text.includes("economia")) return "global-market";
    return "tech";
}

function buildTagSuggestion(topic, angle) {
    const base = `${topic} ${angle}`
        .toLowerCase()
        .replace(/[^a-z0-9\\s]/g, " ")
        .split(/\\s+/)
        .filter((word) => word.length > 3)
        .slice(0, 5);

    return unique(["analise", "noticias", ...base]);
}

function capitalize(value) {
    return value.charAt(0).toUpperCase() + value.slice(1);
}
