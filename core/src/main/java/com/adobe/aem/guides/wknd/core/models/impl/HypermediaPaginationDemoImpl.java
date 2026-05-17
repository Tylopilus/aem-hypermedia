package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.apache.sling.models.factory.ModelFactory;

import com.adobe.aem.guides.wknd.core.hypermedia.AbstractHypermediaComponent;
import com.adobe.aem.guides.wknd.core.hypermedia.FragmentCacheMode;
import com.adobe.aem.guides.wknd.core.hypermedia.HxAction;
import com.adobe.aem.guides.wknd.core.models.HypermediaEventsDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaTableDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaTrainingCatalog;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = HypermediaPaginationDemo.class,
    resourceType = HypermediaPaginationDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaPaginationDemoImpl extends AbstractHypermediaComponent implements HypermediaPaginationDemo {
    @ValueMapValue
    private String targetResourcePath;

    @ValueMapValue
    private String targetSelector;

    @ValueMapValue
    private String targetElementId;

    @OSGiService
    private ModelFactory modelFactory;

    private Resource targetResource;
    private String targetParamPrefix;
    private PaginationConfig pagination;

    @PostConstruct
    protected void initModel() {
        targetResource = resolveTargetResource();
        targetParamPrefix = AbstractHypermediaComponent.paramPrefixFor(targetResource);
        HypermediaPaginationProvider provider = resolveProvider();
        pagination = provider == null ? fallbackPaginationConfig(targetParamPrefix) : provider.getPaginationConfig(request, targetParamPrefix);
    }

    @Override
    public PaginationConfig getPagination() {
        return pagination;
    }

    @Override
    public String getLoadMoreFallbackUrl() {
        return loadMoreAction().getFallbackUrl();
    }

    @Override
    public Map<String, String> getLoadMoreAttributes() {
        return loadMoreAction().getAttributes();
    }

    @Override
    public List<PageLink> getPageLinks() {
        List<PageLink> links = new ArrayList<>();
        for (Integer pageNumber : pagination.pageNumbers()) {
            links.add(pageLink(String.valueOf(pageNumber), pageNumber, pageNumber == pagination.currentPage(), true));
        }
        return links;
    }

    @Override
    public PageLink getPreviousLink() {
        return pageLink("Previous", pagination.currentPage() - 1, false, pagination.hasPreviousPage());
    }

    @Override
    public PageLink getNextLink() {
        return pageLink("Next", pagination.currentPage() + 1, false, pagination.hasNextPage());
    }

    private PageLink pageLink(String label, int pageNumber, boolean current, boolean enabled) {
        if (!enabled || current) {
            return new PageLink(label, "", Map.of(), current, enabled);
        }
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put(pagination.pageParam(), String.valueOf(pageNumber));
        overrides.put(pagination.limitParam(), "");
        HxAction action = hxAction(overrides);
        return new PageLink(label, action.getFallbackUrl(), action.getAttributes(), current, true);
    }

    private HxAction loadMoreAction() {
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put(pagination.limitParam(), String.valueOf(pagination.nextLimit()));
        overrides.put(pagination.pageParam(), "");
        return hxAction(overrides);
    }

    private HxAction hxAction(Map<String, String> overrides) {
        String fallbackUrl = pageUrlWithQuery(overrides);
        return hx()
            .fallbackUrl(fallbackUrl)
            .get(targetFragmentUrlWithQuery(overrides))
            .target(targetElement())
            .swapOuter()
            .pushUrl(fallbackUrl)
            .build();
    }

    private String targetFragmentUrlWithQuery(Map<String, String> overrides) {
        String url = fragmentUrlWithQuery(targetResource, overrides, FragmentCacheMode.CACHEABLE);
        String selector = targetSelector();
        if (selector.isBlank()) {
            return url;
        }
        int queryStart = url.indexOf('?');
        String path = queryStart < 0 ? url : url.substring(0, queryStart);
        String query = queryStart < 0 ? "" : url.substring(queryStart);
        if (!path.endsWith(".html")) {
            return url;
        }
        return path.substring(0, path.length() - ".html".length()) + "." + selector + ".html" + query;
    }

    private String targetElement() {
        if (targetElementId == null || targetElementId.isBlank()) {
            if (HypermediaTrainingCatalog.RESOURCE_TYPE.equals(targetResourceType())) {
                return componentTarget(targetResource) + "-results-content";
            }
            return componentTarget(targetResource);
        }
        return "#" + targetElementId;
    }

    private String targetSelector() {
        if (targetSelector != null && !targetSelector.isBlank()) {
            return targetSelector;
        }
        return HypermediaTrainingCatalog.RESOURCE_TYPE.equals(targetResourceType()) ? "results" : "";
    }

    private String targetResourceType() {
        return targetResource == null ? "" : targetResource.getResourceType();
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

    private HypermediaPaginationProvider resolveProvider() {
        if (targetResource == null || modelFactory == null) {
            return null;
        }
        try {
            return modelFactory.getModelFromWrappedRequest(request, targetResource, HypermediaPaginationProvider.class);
        } catch (RuntimeException e) {
            return targetResource.adaptTo(HypermediaPaginationProvider.class);
        }
    }

    private PaginationConfig fallbackPaginationConfig(String paramPrefix) {
        String targetResourceType = targetResource == null ? "" : targetResource.getResourceType();
        if (HypermediaEventsDemo.RESOURCE_TYPE.equals(targetResourceType)) {
            return HypermediaEventsProviderImpl.paginationConfig(targetResource, request, 0, paramPrefix);
        }
        if (HypermediaTableDemo.RESOURCE_TYPE.equals(targetResourceType)) {
            return HypermediaTableProviderImpl.paginationConfig(targetResource, request, 0, paramPrefix);
        }
        return PaginationConfig.disabled();
    }
}
