package com.adobe.aem.guides.wknd.core.models;

import java.util.List;

public interface HypermediaMultiSelectFilter {
    String RESOURCE_TYPE = "wknd/components/hypermedia-multi-select-filter";

    String getName();
    String getLabel();
    List<HypermediaTrainingCatalog.FilterOption> getOptions();
}
