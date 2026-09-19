'use strict';

const $ = (id) => document.getElementById(id);
const genres = ['ACTION', 'WESTERN', 'TRAGEDY', 'THRILLER'];
const colors = ['RED', 'BLACK', 'BROWN'];
const ratings = ['PG', 'R', 'NC_17'];
const countries = ['UNITED_KINGDOM', 'GERMANY', 'FRANCE', 'THAILAND'];
const names = {
    id: 'ID',
    name: 'Название',
    coordinates: 'Координаты',
    creationDate: 'Дата создания',
    oscarsCount: 'Оскары',
    budget: 'Бюджет',
    totalBoxOffice: 'Сборы',
    mpaaRating: 'Рейтинг',
    director: 'Режиссёр',
    screenwriter: 'Сценарист',
    operator: 'Оператор',
    length: 'Продолжительность',
    goldenPalmCount: 'Золотые пальмы',
    genre: 'Жанр'
};
let csrf = '';
let user = '';
let page = 0;
let current = 'movies';
let refs = {};
let editing = null;
let selected = null;
let deleting = null;
let special = null;

async function api(path, method = 'GET', body) {
    const response = await fetch('api/' + path, {
        method,
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
            'X-CSRF-Token': csrf
        },
        body: body === undefined ? undefined : JSON.stringify(body)
    });
    const text = await response.text();
    let data;
    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        throw new Error(
            response.status === 400
                ? 'Проверьте типы и допустимые значения полей'
                : 'Не удалось прочитать ответ сервера'
        );
    }
    if (!response.ok) {
        if (response.status === 401 && user) {
            user = '';
            showLogin();
        }
        throw new Error(data?.error || 'Ошибка запроса');
    }
    return data;
}

function option(select, value, label) {
    const node = document.createElement('option');
    node.value = value;
    node.textContent = label;
    select.append(node);
}

function showLogin() {
    for (const id of ['movies', 'references', 'special', 'menu']) {
        $(id).hidden = true;
    }
    document.querySelectorAll('dialog[open]').forEach((dialog) => dialog.close());
    $('login').hidden = false;
}

async function loginDone(state) {
    csrf = state.csrf;
    user = state.user;
    if (!user) {
        return showLogin();
    }
    $('login').hidden = true;
    $('menu').hidden = false;
    $('user').textContent = user;
    await navigate(current);
}

async function navigate(name) {
    current = name;
    for (const id of ['movies', 'references', 'special']) {
        $(id).hidden = id !== name;
    }
    await refresh();
}

function button(label, action) {
    const node = document.createElement('button');
    node.textContent = label;
    node.addEventListener('click', () => run(action));
    return node;
}

function value(key, val) {
    if (val == null) {
        return 'Нет';
    }
    if (key === 'coordinates') {
        return `#${val.id}: ${val.x}, ${val.y}`;
    }
    if (['director', 'screenwriter', 'operator'].includes(key)) {
        return `#${val.id}: ${val.name}`;
    }
    return String(val);
}

function moviesTable(items, target) {
    const table = document.createElement('table');
    const head = table.createTHead().insertRow();
    for (const label of [...Object.values(names), 'Действия']) {
        const cell = document.createElement('th');
        cell.textContent = label;
        head.append(cell);
    }
    for (const movie of items) {
        const row = table.insertRow();
        for (const key of Object.keys(names)) {
            row.insertCell().textContent = value(key, movie[key]);
        }
        const cell = row.insertCell();
        cell.append(
            button('Открыть', () => details(movie.id)),
            button('Изменить', () => editMovie(movie.id)),
            button('Удалить', () => {
                deleting = movie;
                $('confirm-text').textContent = `Удалить фильм #${movie.id}: ${movie.name}?`;
                $('delete-error').textContent = '';
                $('confirm').showModal();
            })
        );
    }
    target.replaceChildren(table);
    if (!items.length) {
        const note = document.createElement('p');
        note.textContent = 'Нет объектов';
        target.append(note);
    }
}

async function loadMovies() {
    const form = $('filters');
    const query = new URLSearchParams({
        page,
        size: 10,
        field: form.elements.field.value,
        sort: form.elements.sort.value,
        order: form.elements.order.value
    });
    if (form.elements.value.value !== '') {
        query.set('value', form.elements.value.value);
    }
    const data = await api('movies?' + query);
    if (page > 0 && page * 10 >= data.total) {
        page--;
        return loadMovies();
    }
    moviesTable(data.items, $('movie-list'));
    $('paging').textContent = `Страница ${page + 1}. Всего фильмов: ${data.total}`;
    $('prev').disabled = page === 0;
    $('next').disabled = (page + 1) * 10 >= data.total;
}

function describe(movie) {
    const list = document.createElement('dl');
    function item(label, val) {
        const term = document.createElement('dt');
        const desc = document.createElement('dd');
        term.textContent = label;
        desc.textContent = val;
        list.append(term, desc);
    }
    for (const key of Object.keys(names)) {
        const person = movie[key];
        item(names[key], value(key, person));
        if (['director', 'screenwriter', 'operator'].includes(key) && person) {
            item('Глаза / волосы', `${person.eyeColor} / ${person.hairColor}`);
            item('Рост / страна', `${person.height ?? 'Нет'} / ${person.nationality ?? 'Нет'}`);
            item(
                'Место',
                person.location
                    ? `#${person.location.id}: ${person.location.x}, ${person.location.y}, ${person.location.z}`
                    : 'Нет'
            );
        }
    }
    $('detail-body').replaceChildren(list);
    $('detail-status').textContent = '';
    $('detail-edit').disabled = false;
}

async function details(id) {
    selected = id;
    describe(await api('movies/' + id));
    $('details').showModal();
}

function field(key, label, type = 'text', required = false, values = null, min = null, max = null) {
    const wrap = document.createElement('label');
    wrap.textContent = label;
    const input = document.createElement(values ? 'select' : 'input');
    input.name = key;
    input.required = required;
    if (values) {
        let hint = 'Нет';
        if (values.length === 0) {
            hint = 'Нет созданных записей';
        } else if (required) {
            hint = 'Выберите';
        }
        option(input, '', hint);
        input.disabled = values.length === 0;
        values.forEach((item) => option(input, item.value ?? item, item.label ?? item));
    } else {
        input.type = type;
        if (min !== null) {
            input.min = min;
        }
        if (max !== null) {
            input.max = max;
        }
        if (type === 'number') {
            input.step = '1';
        }
    }
    wrap.append(input);
    $('fields').append(wrap);
    return input;
}

function startEditor(kind, title, id = null, version = null) {
    editing = { kind, id, version };
    $('fields').replaceChildren();
    $('edit-error').textContent = '';
    $('editor-title').textContent = title;
    $('editor-help').replaceChildren();
    $('edit-save').disabled = false;
}

async function editMovie(id = null) {
    const movie = id === null ? {} : await api('movies/' + id);
    refs = await api('references');
    startEditor('movies', id === null ? 'Новый фильм' : 'Изменить фильм', id, movie.version);
    if (!refs.coordinates.length || !refs.people.length) {
        const note = document.createElement('p');
        note.textContent =
            'Сначала создайте координаты и человека в разделе «Связанные объекты». Человек нужен для роли оператора. Режиссёра и сценариста можно не указывать.';
        $('editor-help').append(
            note,
            button('Создать связанные объекты', async () => {
                $('editor').close();
                await navigate('references');
            })
        );
        $('edit-save').disabled = true;
    }
    field('name', 'Название', 'text', true);
    field(
        'coordinates',
        'Координаты',
        'number',
        true,
        refs.coordinates.map((c) => ({ value: c.id, label: `#${c.id}: ${c.x}, ${c.y}` }))
    );
    for (const key of ['oscarsCount', 'budget', 'totalBoxOffice', 'length', 'goldenPalmCount']) {
        field(
            key,
            names[key],
            'number',
            ['budget', 'totalBoxOffice', 'length'].includes(key),
            null,
            1,
            ['budget', 'totalBoxOffice'].includes(key) ? 2147483647 : '9223372036854775807'
        );
    }
    field('mpaaRating', 'Рейтинг', 'text', true, ratings);
    for (const key of ['director', 'screenwriter', 'operator']) {
        field(
            key,
            names[key],
            'number',
            key === 'operator',
            refs.people.map((p) => ({ value: p.id, label: `#${p.id}: ${p.name}` }))
        );
    }
    field('genre', 'Жанр', 'text', true, genres);
    for (const input of $('edit-form').elements) {
        if (!input.name) {
            continue;
        }
        const val = movie[input.name];
        input.value = val?.id ?? val ?? '';
    }
    if ($('details').open) {
        $('details').close();
    }
    $('editor').showModal();
}

async function editReference(kind) {
    refs = await api('references');
    startEditor(
        kind,
        { people: 'Новый человек', coordinates: 'Новые координаты', locations: 'Новое место' }[kind]
    );
    if (kind === 'people') {
        field('name', 'Имя', 'text', true);
        field('eyeColor', 'Цвет глаз', 'text', true, colors);
        field('hairColor', 'Цвет волос', 'text', true, colors);
        field(
            'location',
            'Место',
            'number',
            false,
            refs.locations.map((l) => ({ value: l.id, label: `#${l.id}: ${l.x}, ${l.y}, ${l.z}` }))
        );
        field('height', 'Рост', 'number', false, null, 0.000001).step = 'any';
        field('nationality', 'Страна', 'text', false, countries);
    } else if (kind === 'coordinates') {
        field('x', 'X больше −299', 'number', true, null, -298, 2147483647);
        field('y', 'Y не больше 40', 'number', true, null, '-9223372036854775808', 40);
    } else {
        for (const key of ['x', 'y', 'z']) {
            field(key, key.toUpperCase(), 'number', true).step = 'any';
        }
    }
    $('editor').showModal();
}

async function loadReferences() {
    refs = await api('references');
    $('reference-list').replaceChildren();
    for (const [key, label] of [
        ['locations', 'Места'],
        ['coordinates', 'Координаты'],
        ['people', 'Люди']
    ]) {
        const title = document.createElement('h3');
        title.textContent = label;
        const list = document.createElement('ul');
        refs[key].forEach((row) => {
            const item = document.createElement('li');
            if (row.name) {
                item.textContent = `#${row.id}: ${row.name}`;
            } else {
                item.textContent = `#${row.id}: ${row.x}, ${row.y}`;
                if (row.z !== undefined) {
                    item.textContent += `, ${row.z}`;
                }
            }
            list.append(item);
        });
        if (!refs[key].length) {
            const item = document.createElement('li');
            item.textContent = 'Пока нет записей';
            list.append(item);
        }
        $('reference-list').append(title, list);
    }
}

async function loadSpecial() {
    if (!special) {
        return;
    }
    const data = await api('special/' + special);
    if (Array.isArray(data)) {
        moviesTable(data, $('special-result'));
    } else {
        $('special-result').textContent =
            data.average === null ? 'Нет заполненных значений' : `Среднее: ${data.average}`;
    }
}

async function refresh() {
    if (!user) {
        return;
    }
    if (current === 'movies') {
        await loadMovies();
    }
    if (current === 'references') {
        await loadReferences();
    }
    if (current === 'special') {
        await loadSpecial();
    }
    if ($('details').open) {
        try {
            describe(await api('movies/' + selected));
        } catch (error) {
            $('detail-status').textContent = error.message;
            $('detail-body').replaceChildren();
            $('detail-edit').disabled = true;
        }
    }
}

async function run(action) {
    try {
        await action();
        $('notice').textContent = '';
    } catch (error) {
        $('notice').textContent = error.message;
    }
}

$('login-form').addEventListener('submit', (event) => {
    event.preventDefault();
    run(async () => {
        const state = await api('session');
        csrf = state.csrf;
        const credentials = Object.fromEntries(new FormData(event.target));
        const path = 'session/' + event.submitter.value;
        const result = await api(path, 'POST', credentials);
        await loginDone(result);
        event.target.reset();
    });
});
$('logout').onclick = () =>
    run(async () => {
        await api('session', 'DELETE');
        user = '';
        showLogin();
    });
for (const node of document.querySelectorAll('[data-page]')) {
    node.onclick = () => run(() => navigate(node.dataset.page));
}
for (const node of document.querySelectorAll('[data-close]')) {
    node.onclick = () => $(node.dataset.close).close();
}
for (const node of document.querySelectorAll('[data-ref]')) {
    node.onclick = () => run(() => editReference(node.dataset.ref));
}
for (const node of document.querySelectorAll('[data-special]')) {
    node.onclick = () =>
        run(async () => {
            special = node.dataset.special;
            await loadSpecial();
        });
}
$('new-movie').onclick = () => run(() => editMovie());
$('detail-edit').onclick = () => run(() => editMovie(selected));
$('lookup').onsubmit = (event) => {
    event.preventDefault();
    run(() => details(event.target.elements.id.value));
};
$('filters').onsubmit = (event) => {
    event.preventDefault();
    page = 0;
    run(loadMovies);
};
$('prev').onclick = () => {
    page--;
    run(loadMovies);
};
$('next').onclick = () => {
    page++;
    run(loadMovies);
};
$('genres').onsubmit = (event) => {
    event.preventDefault();
    special = 'genres?genre=' + event.target.elements.genre.value;
    run(loadSpecial);
};
$('award').onsubmit = (event) => {
    event.preventDefault();
    run(async () => {
        const result = await api('special/award', 'POST', {
            length: event.target.elements.length.value,
            amount: event.target.elements.amount.value
        });
        special = null;
        $('special-result').textContent = `Награждено фильмов: ${result.updated}`;
    });
};
$('delete-confirm').onclick = async () => {
    try {
        await api(`movies/${deleting.id}?version=${deleting.version}`, 'DELETE');
        $('confirm').close();
        await refresh();
    } catch (error) {
        $('delete-error').textContent = error.message;
    }
};
$('edit-form').onsubmit = async (event) => {
    event.preventDefault();
    const data = {};
    const numeric = [
        'coordinates',
        'director',
        'screenwriter',
        'operator',
        'location',
        'oscarsCount',
        'budget',
        'totalBoxOffice',
        'length',
        'goldenPalmCount',
        'height',
        'x',
        'y',
        'z'
    ];
    const longs = ['oscarsCount', 'length', 'goldenPalmCount'];
    if (editing.kind === 'coordinates') {
        longs.push('y');
    }
    for (const [key, val] of new FormData(event.target)) {
        if (val === '') {
            data[key] = null;
        } else if (longs.includes(key)) {
            data[key] = val;
        } else if (numeric.includes(key)) {
            data[key] = Number(val);
        } else {
            data[key] = val;
        }
    }
    if (editing.id !== null) {
        data.version = editing.version;
    }
    try {
        let path = editing.kind;
        let method = 'POST';
        if (editing.id !== null) {
            path += '/' + editing.id;
            method = 'PUT';
        }
        await api(path, method, data);
        $('editor').close();
        await refresh();
    } catch (error) {
        $('edit-error').textContent = error.message;
    }
};
for (const key of ['name', 'mpaaRating', 'genre', 'director', 'screenwriter', 'operator']) {
    option($('filters').elements.field, key, names[key]);
}
option($('filters').elements.sort, 'id', 'ID');
for (const key of ['name', 'mpaaRating', 'genre', 'director', 'screenwriter', 'operator']) {
    option($('filters').elements.sort, key, names[key]);
}
genres.forEach((genre) => option($('genres').elements.genre, genre, genre));
run(async () => loginDone(await api('session')));
setInterval(() => {
    if (user && !document.hidden) {
        run(refresh);
    }
}, 3000);
