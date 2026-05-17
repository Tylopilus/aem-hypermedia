package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.aem.guides.wknd.core.hypermedia.AbstractHypermediaComponent;
import com.adobe.aem.guides.wknd.core.hypermedia.FragmentCacheMode;
import com.adobe.aem.guides.wknd.core.hypermedia.FragmentUrlService;
import com.adobe.aem.guides.wknd.core.hypermedia.HxAction;
import com.adobe.aem.guides.wknd.core.models.BaseFilterModel;
import com.adobe.aem.guides.wknd.core.models.Filter;
import com.adobe.aem.guides.wknd.core.models.HypermediaEventsDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaTableDemo;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;
import com.adobe.aem.guides.wknd.core.models.Search;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = HypermediaFilterDemo.class,
    resourceType = HypermediaFilterDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaFilterDemoImpl extends AbstractHypermediaComponent implements HypermediaFilterDemo {
    @ValueMapValue
    @Default(values = "Filters")
    private String title;

    @ValueMapValue
    private String targetResourcePath;

    @OSGiService
    private ModelFactory modelFactory;

    private Resource targetResource;
    private String targetParamPrefix;
    private BaseFilterModel filtersConfig;
    private List<FilterView> filters;
    private List<String> controlledParams;

    @PostConstruct
    protected void initModel() {
        targetResource = resolveTargetResource();
        targetParamPrefix = AbstractHypermediaComponent.paramPrefixFor(targetResource);
        HypermediaFilterProvider provider = resolveFilterProvider();
        filtersConfig = provider == null
            ? fallbackFiltersConfig(targetParamPrefix)
            : provider.getFiltersConfig(targetParamPrefix);
        if (filtersConfig.filters().isEmpty()) {
            filtersConfig = fallbackFiltersConfig(targetParamPrefix);
        }
        filters = filtersConfig.filters().stream()
            .map(filter -> new FilterView(filter, request.getParameter(filter.name())))
            .toList();
        controlledParams = new java.util.ArrayList<>(filtersConfig.filters().stream()
            .map(Filter::name)
            .toList());
        HypermediaPaginationProvider paginationProvider = resolvePaginationProvider();
        PaginationConfig paginationConfig = paginationProvider == null ? PaginationConfig.disabled() : paginationProvider.getPaginationConfig(request, targetParamPrefix);
        controlledParams.add(paginationConfig.pageParam());
        controlledParams.add(paginationConfig.limitParam());
    }

    @Override
    public String getTitle() {
        if (title == null || title.isBlank() || "Filters".equals(title)) {
            return HypermediaTableDemo.RESOURCE_TYPE.equals(targetResource == null ? "" : targetResource.getResourceType())
                ? "Table filters"
                : "Event filters";
        }
        return title;
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
    public Map<String, String> getPreservedQueryParams() {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (!controlledParams.contains(key) && values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    @Override
    public List<FilterView> getFilters() {
        return filters;
    }

    @Override
    public Search getSearch() {
        return filtersConfig.search();
    }

    private HxAction getFilterAction() {
        Map<String, String> clearedParams = clearedControlledParams();
        String fallbackUrl = pageUrlWithQuery(clearedParams);
        return hx()
            .fallbackUrl(fallbackUrl)
            .get(fragmentUrlWithQuery(targetResource, clearedParams, FragmentCacheMode.CACHEABLE))
            .target(componentTarget(targetResource))
            .swapOuter()
            .pushUrl(fallbackUrl)
            .trigger("change")
            .build();
    }

    private HxAction getRefreshAction() {
        String fallbackUrl = pageUrlWithQuery(Map.of());
        return hx()
            .fallbackUrl(fallbackUrl)
            .get(fragmentUrlWithQuery(targetResource, Map.of(), FragmentCacheMode.NON_CACHEABLE))
            .headers("{\"" + FragmentUrlService.NON_CACHEABLE_HEADER + "\":\"non-cacheable\"}")
            .target(componentTarget(targetResource))
            .swapOuter()
            .pushUrl(fallbackUrl)
            .build();
    }

    private Map<String, String> clearedControlledParams() {
        Map<String, String> params = new LinkedHashMap<>();
        controlledParams.forEach(name -> params.put(name, ""));
        return params;
    }

    private Resource resolveTargetResource() {
        if (targetResourcePath == null || targetResourcePath.isBlank()) {
            return resource.getParent();
        }
        if (targetResourcePath.startsWith("/")) {
            return resource.getResourceResolver().getResource(targetResourcePath);
        }
        Resource parent = resource.getParent();
        if (parent == null) {
            return null;
        }
        String relativePath = targetResourcePath.startsWith("./") ? targetResourcePath.substring(2) : targetResourcePath;
        return resource.getResourceResolver().getResource(parent.getPath() + "/" + relativePath);
    }

    private HypermediaFilterProvider resolveFilterProvider() {
        if (targetResource == null) {
            return null;
        }
        if (modelFactory != null) {
            try {
                return modelFactory.getModelFromWrappedRequest(request, targetResource, HypermediaFilterProvider.class);
            } catch (RuntimeException e) {
                // fallback to adaptTo
            }
        }
        return targetResource.adaptTo(HypermediaFilterProvider.class);
    }

    private HypermediaPaginationProvider resolvePaginationProvider() {
        if (targetResource == null) {
            return null;
        }
        if (modelFactory != null) {
            try {
                return modelFactory.getModelFromWrappedRequest(request, targetResource, HypermediaPaginationProvider.class);
            } catch (RuntimeException e) {
                // fallback to adaptTo
            }
        }
        return targetResource.adaptTo(HypermediaPaginationProvider.class);
    }

    private BaseFilterModel fallbackFiltersConfig(String paramPrefix) {
        String targetResourceType = targetResource == null ? "" : targetResource.getResourceType();
        if (HypermediaEventsDemo.RESOURCE_TYPE.equals(targetResourceType)) {
            return HypermediaEventsProviderImpl.filtersConfig(targetResource, paramPrefix);
        }
        if (HypermediaTableDemo.RESOURCE_TYPE.equals(targetResourceType)) {
            return HypermediaTableProviderImpl.filtersConfig(paramPrefix);
        }
        return new BaseFilterModel(List.of(), Search.disabled());
    }
}
