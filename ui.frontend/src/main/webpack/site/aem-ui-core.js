export class AemElement extends HTMLElement {
    connectedCallback() {
        if (this.connected) {
            return;
        }
        this.connected = true;
        this.onConnect();
    }

    disconnectedCallback() {
        this.connected = false;
        this.onDisconnect();
    }

    onConnect() {
        return undefined;
    }

    onDisconnect() {
        return undefined;
    }
}

export function registerAemComponent(name, elementClass) {
    if (!customElements.get(name)) {
        customElements.define(name, elementClass);
    }
}

function urlWithCurrentQuery(baseUrl) {
    const url = new URL(baseUrl, window.location.href);
    new URLSearchParams(window.location.search).forEach((value, key) => {
        if (!url.searchParams.has(key)) {
            url.searchParams.set(key, value);
        }
    });
    return url;
}

function requestUrlWithFormData(baseUrl, form) {
    const url = urlWithCurrentQuery(baseUrl);
    const data = new FormData(form);
    const keysToClear = new Set();
    data.forEach((value, key) => {
        keysToClear.add(key);
    });
    keysToClear.forEach(key => url.searchParams.delete(key));
    data.forEach((value, key) => {
        if (value) {
            url.searchParams.append(key, value);
        }
    });
    return url;
}

function requestUrlWithLinkData(link) {
    const url = urlWithCurrentQuery(link.getAttribute('hx-get'));
    clearParamsFor(link, url);
    return url;
}

function clearParamsFor(source, url) {
    const clearParams = source.getAttribute('hx-clear-params');
    if (!clearParams) {
        return;
    }
    clearParams.split(/[\s,]+/).filter(Boolean).forEach(key => url.searchParams.delete(key));
}

function headersFor(source) {
    const headersJson = source.getAttribute('hx-headers');
    const headers = { 'X-Requested-With': 'aem-hypermedia' };
    if (!headersJson) {
        return headers;
    }
    try {
        Object.assign(headers, JSON.parse(headersJson));
    } catch (e) {
        // eslint-disable-next-line no-console
        console.warn('Failed to parse hx-headers', e);
    }
    return headers;
}

function targetFor(source) {
    const selector = source.getAttribute('hx-target');
    if (!selector || selector === 'this') {
        return source;
    }
    return document.querySelector(selector);
}

function pushBrowserUrl(source, requestUrl) {
    const pushUrl = source.getAttribute('hx-push-url');
    if (!pushUrl || pushUrl === 'false') {
        return;
    }
    if (pushUrl === 'true') {
        window.history.pushState({}, '', requestUrl.toString());
        return;
    }
    if (source instanceof HTMLFormElement) {
        window.history.pushState({}, '', requestUrlWithFormData(pushUrl, source).toString());
        return;
    }
    const url = urlWithCurrentQuery(pushUrl);
    clearParamsFor(source, url);
    window.history.pushState({}, '', url.toString());
}

async function swapHypermediaFragment(source, requestUrl) {
    const target = targetFor(source);
    if (!requestUrl || !target) {
        return;
    }
    document.body.dispatchEvent(new CustomEvent('aem:request:start', { bubbles: true, detail: { url: requestUrl.toString() } }));

    try {
        const response = await fetch(requestUrl.toString(), { headers: headersFor(source) });
        if (!response.ok) {
            throw new Error(`Fragment request failed with ${response.status}`);
        }
        const html = await response.text();
        const template = document.createElement('template');
        template.innerHTML = html.trim();
        const replacement = template.content.firstElementChild;
        if (!replacement) {
            return;
        }
        target.replaceWith(replacement);

        pushBrowserUrl(source, requestUrl);
        document.body.dispatchEvent(new CustomEvent('aem:fragment:swap', { bubbles: true, detail: { target: replacement } }));
    } catch (error) {
        document.body.dispatchEvent(new CustomEvent('aem:request:error', { bubbles: true, detail: { error } }));
    }
}

document.addEventListener('submit', (event) => {
    const form = event.target.closest('form[hx-get]');
    if (!form) {
        return;
    }
    event.preventDefault();
    submitHypermediaForm(form);
});

function submitHypermediaForm(form) {
    const endpoint = form.getAttribute('hx-get');
    if (endpoint) {
        swapHypermediaFragment(form, requestUrlWithFormData(endpoint, form));
    }
}

const inputSubmitTimers = new WeakMap();

function hypermediaFormForControl(control) {
    const form = control.closest('form[hx-get][hx-trigger~="change"]') || control.form;
    return form && form.matches('form[hx-get][hx-trigger~="change"]') ? form : null;
}

document.addEventListener('change', (event) => {
    const form = hypermediaFormForControl(event.target);
    if (form) {
        submitHypermediaForm(form);
    }
});

document.addEventListener('input', (event) => {
    if (!event.target.matches('input[type="search"]')) {
        return;
    }
    const form = hypermediaFormForControl(event.target);
    if (!form) {
        return;
    }
    window.clearTimeout(inputSubmitTimers.get(event.target));
    inputSubmitTimers.set(event.target, window.setTimeout(() => submitHypermediaForm(form), 300));
});

document.addEventListener('click', (event) => {
    const link = event.target.closest('a[hx-get]');
    if (!link) {
        return;
    }
    event.preventDefault();
    swapHypermediaFragment(link, requestUrlWithLinkData(link));
});
