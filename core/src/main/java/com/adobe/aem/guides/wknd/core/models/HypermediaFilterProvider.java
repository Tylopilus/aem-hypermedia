package com.adobe.aem.guides.wknd.core.models;

public interface HypermediaFilterProvider {
    BaseFilterModel getFiltersConfig();

    default BaseFilterModel getFiltersConfig(String paramPrefix) {
        return getFiltersConfig();
    }
}
