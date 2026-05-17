package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.aem.guides.wknd.core.hypermedia.AbstractHypermediaComponent;
import com.adobe.aem.guides.wknd.core.hypermedia.FragmentCacheMode;
import com.adobe.aem.guides.wknd.core.hypermedia.HxAction;
import com.adobe.aem.guides.wknd.core.models.BaseFilterModel;
import com.adobe.aem.guides.wknd.core.models.HypermediaMultiSelectFilter;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaTrainingCatalog;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;
import com.adobe.aem.guides.wknd.core.models.Search;
import com.adobe.aem.guides.wknd.core.service.TrainingCatalogService;
import com.adobe.aem.guides.wknd.core.service.TrainingItem;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { HypermediaTrainingCatalog.class, HypermediaPaginationProvider.class },
    resourceType = HypermediaTrainingCatalog.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaTrainingCatalogImpl extends AbstractHypermediaComponent implements HypermediaTrainingCatalog {

    @OSGiService
    private TrainingCatalogService trainingCatalogService;

    private List<TrainingItem> allTrainings;
    private List<TrainingView> trainings;
    private int totalTrainings;
    private List<FilterGroup> filterGroups;
    private List<Resource> filterResources;
    private Resource paginationResource;
    private Search search;
    private List<String> controlledParams;

    @PostConstruct
    protected void initModel() {
        allTrainings = trainingCatalogService.getTrainings();
        String prefix = getParamPrefix();

        List<String> typeValues = paramValues(request, prefix + HypermediaTrainingCatalogProviderImpl.TYPE_PARAM);
        List<String> productValues = paramValues(request, prefix + HypermediaTrainingCatalogProviderImpl.PRODUCT_PARAM);
        List<String> langValues = paramValues(request, prefix + HypermediaTrainingCatalogProviderImpl.LANGUAGE_PARAM);
        List<TrainingItem> filtered = HypermediaTrainingCatalogProviderImpl.filteredTrainings(allTrainings, request, prefix);

        totalTrainings = filtered.size();
        PaginationConfig pagination = getPaginationConfig(request);
        int start = pagination.pages() ? Math.min((pagination.currentPage() - 1) * pagination.pageSize(), totalTrainings) : 0;
        int end = pagination.loadMore()
            ? Math.min(pagination.currentLimit(), totalTrainings)
            : Math.min(start + pagination.pageSize(), totalTrainings);
        trainings = filtered.stream()
            .skip(start)
            .limit(Math.max(0, end - start))
            .map(this::toView)
            .toList();

        filterGroups = List.of(
            new FilterGroup(prefix + HypermediaTrainingCatalogProviderImpl.TYPE_PARAM, "Training Type",
                uniqueOptions(allTrainings, TrainingItem::types, typeValues)),
            new FilterGroup(prefix + HypermediaTrainingCatalogProviderImpl.PRODUCT_PARAM, "Product",
                uniqueOptions(allTrainings, TrainingItem::products, productValues)),
            new FilterGroup(prefix + HypermediaTrainingCatalogProviderImpl.LANGUAGE_PARAM, "Language",
                uniqueOptions(allTrainings, TrainingItem::languages, langValues))
        );
        filterResources = filterGroups.stream()
            .map(this::toFilterResource)
            .toList();
        paginationResource = toPaginationResource();

        search = HypermediaTrainingCatalogProviderImpl.filtersConfig(allTrainings, prefix).search();

        controlledParams = new ArrayList<>(List.of(
            prefix + HypermediaTrainingCatalogProviderImpl.TYPE_PARAM,
            prefix + HypermediaTrainingCatalogProviderImpl.PRODUCT_PARAM,
            prefix + HypermediaTrainingCatalogProviderImpl.LANGUAGE_PARAM,
            prefix + HypermediaTrainingCatalogProviderImpl.SEARCH_PARAM,
            prefix + HypermediaTrainingCatalogProviderImpl.PAGE_PARAM,
            prefix + HypermediaTrainingCatalogProviderImpl.LIMIT_PARAM
        ));
    }

    @Override
    public List<TrainingView> getTrainings() {
        return trainings;
    }

    @Override
    public String getFormId() {
        return getId() + "-form";
    }

    @Override
    public String getResultsId() {
        return getId() + "-results";
    }

    @Override
    public String getResultsContentId() {
        return getResultsId() + "-content";
    }

    @Override
    public boolean isResultsRequest() {
        if (request == null || request.getRequestPathInfo() == null) {
            return false;
        }
        for (String selector : request.getRequestPathInfo().getSelectors()) {
            if ("results".equals(selector)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTotalTrainings() {
        return totalTrainings;
    }

    @Override
    public List<FilterGroup> getFilterGroups() {
        return filterGroups;
    }

    @Override
    public List<Resource> getFilterResources() {
        return filterResources;
    }

    @Override
    public Resource getPaginationResource() {
        return paginationResource;
    }

    @Override
    public Search getSearch() {
        return search;
    }

    @Override
    public String getFilterActionFallbackUrl() {
        return getFilterAction().getFallbackUrl();
    }

    @Override
    public Map<String, String> getFilterActionAttributes() {
        return getFilterAction().getAttributes();
    }

    @Override
    public String getRefreshActionFallbackUrl() {
        return getRefreshAction().getFallbackUrl();
    }

    @Override
    public Map<String, String> getRefreshActionAttributes() {
        return getRefreshAction().getAttributes();
    }

    @Override
    public Map<String, List<String>> getPreservedQueryParams() {
        Map<String, List<String>> params = new LinkedHashMap<>();
        if (request != null) {
            request.getParameterMap().forEach((key, values) -> {
                if (!controlledParams.contains(key) && values != null && values.length > 0) {
                    params.put(key, Arrays.asList(values));
                }
            });
        }
        return params;
    }

    @Override
    public Map<String, String> getResetPaginationParams() {
        String prefix = getParamPrefix();
        return Map.of(
            prefix + HypermediaTrainingCatalogProviderImpl.PAGE_PARAM, "1",
            prefix + HypermediaTrainingCatalogProviderImpl.LIMIT_PARAM, ""
        );
    }

    @Override
    public BaseFilterModel getFiltersConfig() {
        return getFiltersConfig(getParamPrefix());
    }

    @Override
    public BaseFilterModel getFiltersConfig(String paramPrefix) {
        return HypermediaTrainingCatalogProviderImpl.filtersConfig(allTrainings, paramPrefix);
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request) {
        return getPaginationConfig(request, getParamPrefix());
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request, String paramPrefix) {
        return HypermediaTrainingCatalogProviderImpl.paginationConfig(
            request,
            HypermediaTrainingCatalogProviderImpl.paginationType(resource),
            HypermediaTrainingCatalogProviderImpl.pageSize(resource),
            totalTrainings,
            paramPrefix
        );
    }

    private HxAction getFilterAction() {
        Map<String, String> clearedParams = clearedControlledParams();
        Map<String, List<String>> emptyMulti = Map.of();
        String fallbackUrl = pageUrlWithQuery(clearedParams, emptyMulti);
        return hx()
            .fallbackUrl(fallbackUrl)
            .get(resultsFragmentUrlWithQuery(clearedParams, emptyMulti, FragmentCacheMode.CACHEABLE))
            .target("#" + getResultsContentId())
            .swapOuter()
            .trigger("change")
            .pushUrl(fallbackUrl)
            .build();
    }

    private String resultsFragmentUrlWithQuery(Map<String, String> overrides, Map<String, List<String>> multiValueOverrides, FragmentCacheMode cacheMode) {
        return fragmentUrlService.withCurrentQuery(resultsFragmentUrl(cacheMode), request, overrides, multiValueOverrides);
    }

    private String resultsFragmentUrl(FragmentCacheMode cacheMode) {
        String url = fragmentUrl(cacheMode);
        return url.endsWith(".html") ? url.substring(0, url.length() - ".html".length()) + ".results.html" : url;
    }

    private HxAction getRefreshAction() {
        Map<String, String> clearedParams = clearedControlledParams();
        Map<String, List<String>> emptyMulti = Map.of();
        String fallbackUrl = pageUrlWithQuery(clearedParams, emptyMulti);
        return hx()
            .fallbackUrl(fallbackUrl)
            .get(fragmentUrlWithQuery(clearedParams, emptyMulti, FragmentCacheMode.NON_CACHEABLE))
            .headers("{\"" + com.adobe.aem.guides.wknd.core.hypermedia.FragmentUrlService.NON_CACHEABLE_HEADER + "\":\"non-cacheable\"}")
            .target(componentTarget())
            .swapOuter()
            .pushUrl(fallbackUrl)
            .clearParams(String.join(" ", controlledParams))
            .build();
    }

    private Map<String, String> clearedControlledParams() {
        Map<String, String> params = new LinkedHashMap<>();
        controlledParams.forEach(name -> params.put(name, ""));
        return params;
    }

    static List<String> paramValues(SlingHttpServletRequest request, String name) {
        String[] values = request == null ? null : request.getParameterValues(name);
        if (values == null) {
            return List.of();
        }
        return Arrays.stream(values)
            .filter(v -> v != null && !v.isBlank())
            .map(String::trim)
            .map(String::toLowerCase)
            .filter(v -> v.matches("[a-z0-9_:/-]{1,128}"))
            .toList();
    }

    static String paramValue(SlingHttpServletRequest request, String name) {
        String value = request == null ? null : request.getParameter(name);
        return value == null ? "" : value.trim().toLowerCase();
    }

    private TrainingView toView(TrainingItem t) {
        return new TrainingView(
            t.id(), t.title(), t.description(), t.price(), t.duration(),
            t.types(), t.products(), t.languages()
        );
    }

    private List<HypermediaTrainingCatalog.FilterOption> uniqueOptions(List<TrainingItem> items, Function<TrainingItem, List<String>> extractor, List<String> selectedValues) {
        return items.stream()
            .flatMap(t -> extractor.apply(t).stream())
            .distinct()
            .sorted()
            .map(v -> new HypermediaTrainingCatalog.FilterOption(v, capitalize(v), selectedValues.contains(v)))
            .toList();
    }

    private Resource toFilterResource(FilterGroup group) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", group.name());
        properties.put("label", group.label());
        properties.put("options", group.options());
        String path = resource.getPath() + "/filters/" + group.name().replaceAll("[^a-zA-Z0-9_-]", "-");
        return new ValueMapSyntheticResource(resource.getResourceResolver(), path, HypermediaMultiSelectFilter.RESOURCE_TYPE, properties);
    }

    private Resource toPaginationResource() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("targetSelector", "results");
        properties.put("targetElementId", getResultsContentId());
        return new ValueMapSyntheticResource(resource.getResourceResolver(), resource.getPath() + "/pagination", HypermediaPaginationDemo.RESOURCE_TYPE, properties);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private static final class ValueMapSyntheticResource extends SyntheticResource {
        private final ValueMap valueMap;

        private ValueMapSyntheticResource(ResourceResolver resourceResolver, String path, String resourceType, Map<String, Object> properties) {
            super(resourceResolver, path, resourceType);
            this.valueMap = new ValueMapDecorator(properties);
        }

        @Override
        public ValueMap getValueMap() {
            return valueMap;
        }
    }
}
