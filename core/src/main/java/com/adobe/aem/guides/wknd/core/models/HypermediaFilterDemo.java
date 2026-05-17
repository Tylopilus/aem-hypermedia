package com.adobe.aem.guides.wknd.core.models;

import java.util.List;
import java.util.Map;

public interface HypermediaFilterDemo {
    public static final String RESOURCE_TYPE = "wknd/components/hypermedia-filter-demo";

    String getId();
    String getTitle();
    String getFilterActionFallbackUrl();
    Map<String, String> getFilterActionAttributes();
    String getRefreshActionFallbackUrl();
    Map<String, String> getRefreshActionAttributes();
    Map<String, String> getPreservedQueryParams();
    List<FilterView> getFilters();
    Search getSearch();

    public record FilterView(Filter filter, String selectedValue) {}
}
