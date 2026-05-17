import { AemElement, registerAemComponent } from '../../site/aem-ui-core';

class CmpPagination extends AemElement {
    onConnect() {
        this.page = 1;
        this.render();
    }

    render() {
        const list = this.parentElement ? this.parentElement.querySelector(this.dataset.listSelector) : null;
        if (!list) {
            this.innerHTML = '';
            return;
        }

        this.items = Array.from(list.children);
        this.pageSize = Number(this.dataset.pageSize || 6);
        this.totalPages = Math.ceil(this.items.length / this.pageSize);

        if (this.totalPages <= 1) {
            this.items.forEach((item) => item.hidden = false);
            this.innerHTML = '';
            return;
        }

        this.updateItems();
        this.innerHTML = this.controlsMarkup();
        this.querySelectorAll('button').forEach((button) => {
            button.addEventListener('click', () => {
                this.page = Number(button.dataset.page);
                this.render();
                this.dispatchEvent(new CustomEvent('aem:pagination:change', {
                    bubbles: true,
                    detail: { page: this.page, pageSize: this.pageSize }
                }));
            });
        });
    }

    updateItems() {
        const start = (this.page - 1) * this.pageSize;
        const end = start + this.pageSize;
        this.items.forEach((item, index) => item.hidden = index < start || index >= end);
    }

    controlsMarkup() {
        return `<nav class="cmp-pagination" aria-label="Events pagination">${Array.from({ length: this.totalPages }, (_, index) => {
            const page = index + 1;
            const current = page === this.page;
            return `<button type="button" data-page="${page}" aria-current="${current ? 'page' : 'false'}">${page}</button>`;
        }).join('')}</nav>`;
    }
}

registerAemComponent('cmp-pagination', CmpPagination);
