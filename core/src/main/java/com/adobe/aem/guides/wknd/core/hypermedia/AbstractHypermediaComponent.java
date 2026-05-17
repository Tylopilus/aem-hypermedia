package com.adobe.aem.guides.wknd.core.hypermedia;

import javax.annotation.PostConstruct;

import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.WCMMode;

public abstract class AbstractHypermediaComponent {
    @Self
    protected SlingHttpServletRequest request;

    @SlingObject
    protected Resource resource;

    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    protected Page currentPage;

    @OSGiService
    protected FragmentUrlService fragmentUrlService;

    private String id;

    @PostConstruct
    protected void initHypermediaComponent() {
        id = "cmp-" + Math.abs(resource.getPath().hashCode());
    }

    public String getId() {
        return id;
    }

    public static String paramPrefixFor(Resource resource) {
        if (resource == null) {
            return "";
        }
        int hash = resource.getPath().hashCode();
        long absHash = hash == Integer.MIN_VALUE ? ((long) Integer.MAX_VALUE) + 1 : Math.abs(hash);
        return "cmp" + absHash + "_";
    }

    public String getParamPrefix() {
        return paramPrefixFor(resource);
    }

    public String scopedParam(String paramName) {
        return getParamPrefix() + paramName;
    }

    protected HxAction.Builder hx() {
        return HxAction.builder().disabled(!isInteractiveMode());
    }

    protected String componentTarget() {
        return "#" + getId();
    }

    protected String componentTarget(Resource targetResource) {
        if (targetResource == null) {
            return componentTarget();
        }
        return "#cmp-" + Math.abs(targetResource.getPath().hashCode());
    }

    protected String fragmentUrl() {
        return fragmentUrl(FragmentCacheMode.CACHEABLE);
    }

    protected String fragmentUrl(FragmentCacheMode cacheMode) {
        return fragmentUrlService.resourceFragmentUrl(resource, request, cacheMode);
    }

    protected String fragmentUrl(Resource targetResource, FragmentCacheMode cacheMode) {
        return fragmentUrlService.resourceFragmentUrl(targetResource, request, cacheMode);
    }

    protected String pageUrl() {
        if (currentPage == null) {
            return "";
        }
        return fragmentUrlService.pageUrl(currentPage.getPath(), resource.getResourceResolver(), request);
    }

    protected String fragmentUrlWithQuery(Map<String, String> overrides) {
        return fragmentUrlWithQuery(overrides, FragmentCacheMode.CACHEABLE);
    }

    protected String fragmentUrlWithQuery(Map<String, String> overrides, FragmentCacheMode cacheMode) {
        return fragmentUrlService.withCurrentQuery(fragmentUrl(cacheMode), request, overrides);
    }

    protected String fragmentUrlWithQuery(Map<String, String> overrides, Map<String, java.util.List<String>> multiValueOverrides) {
        return fragmentUrlService.withCurrentQuery(fragmentUrl(), request, overrides, multiValueOverrides);
    }

    protected String fragmentUrlWithQuery(Map<String, String> overrides, Map<String, java.util.List<String>> multiValueOverrides, FragmentCacheMode cacheMode) {
        return fragmentUrlService.withCurrentQuery(fragmentUrl(cacheMode), request, overrides, multiValueOverrides);
    }

    protected String fragmentUrlWithQuery(Resource targetResource, Map<String, String> overrides, FragmentCacheMode cacheMode) {
        return fragmentUrlService.withCurrentQuery(fragmentUrl(targetResource, cacheMode), request, overrides);
    }

    protected String pageUrlWithQuery(Map<String, String> overrides) {
        return fragmentUrlService.withCurrentQuery(pageUrl(), request, overrides);
    }

    protected String pageUrlWithQuery(Map<String, String> overrides, Map<String, java.util.List<String>> multiValueOverrides) {
        return fragmentUrlService.withCurrentQuery(pageUrl(), request, overrides, multiValueOverrides);
    }

    protected boolean isInteractiveMode() {
        WCMMode mode = WCMMode.fromRequest(request);
        return mode != WCMMode.EDIT && mode != WCMMode.DESIGN;
    }
}
