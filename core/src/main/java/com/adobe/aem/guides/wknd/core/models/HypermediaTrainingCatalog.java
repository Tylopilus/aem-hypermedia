package com.adobe.aem.guides.wknd.core.models;

import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;

public interface HypermediaTrainingCatalog extends HypermediaFilterProvider, HypermediaPaginationProvider {
    String RESOURCE_TYPE = "wknd/components/hypermedia-training-catalog";

    String getId();
    String getFormId();
    String getResultsId();
    String getResultsContentId();
    boolean isResultsRequest();
    List<TrainingView> getTrainings();
    int getTotalTrainings();
    List<FilterGroup> getFilterGroups();
    List<Resource> getFilterResources();
    Resource getPaginationResource();
    Search getSearch();
    String getFilterActionFallbackUrl();
    Map<String, String> getFilterActionAttributes();
    String getRefreshActionFallbackUrl();
    Map<String, String> getRefreshActionAttributes();
    Map<String, List<String>> getPreservedQueryParams();
    Map<String, String> getResetPaginationParams();

    record TrainingView(String id, String title, String description, String price, String duration,
                        List<String> types, List<String> products, List<String> languages) {
        public TrainingView {
            types = types == null ? List.of() : List.copyOf(types);
            products = products == null ? List.of() : List.copyOf(products);
            languages = languages == null ? List.of() : List.copyOf(languages);
        }
    }

    record FilterOption(String value, String label, boolean selected) {}

    record FilterGroup(String name, String label, List<FilterOption> options) {
        public FilterGroup {
            options = options == null ? List.of() : List.copyOf(options);
        }
    }
}
