package com.adobe.aem.guides.wknd.core.models;

import java.util.List;
import java.util.Map;

public interface HypermediaPaginationDemo {
    String RESOURCE_TYPE = "wknd/components/hypermedia-pagination-demo";

    PaginationConfig getPagination();
    String getLoadMoreFallbackUrl();
    Map<String, String> getLoadMoreAttributes();
    List<PageLink> getPageLinks();
    PageLink getPreviousLink();
    PageLink getNextLink();

    record PageLink(String label, String fallbackUrl, Map<String, String> attributes, boolean current, boolean enabled) {}
}
