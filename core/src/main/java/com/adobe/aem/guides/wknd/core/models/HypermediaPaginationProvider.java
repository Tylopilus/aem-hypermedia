package com.adobe.aem.guides.wknd.core.models;

import org.apache.sling.api.SlingHttpServletRequest;

public interface HypermediaPaginationProvider {
    PaginationConfig getPaginationConfig(SlingHttpServletRequest request);

    default PaginationConfig getPaginationConfig(SlingHttpServletRequest request, String paramPrefix) {
        return getPaginationConfig(request);
    }
}
