package com.adobe.aem.guides.wknd.core.models;

import java.util.List;

public record BaseFilterModel(List<Filter> filters, Search search) {
    public BaseFilterModel {
        filters = filters == null ? List.of() : List.copyOf(filters);
        search = search == null ? Search.disabled() : search;
    }
}
