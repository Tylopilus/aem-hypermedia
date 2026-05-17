package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.aem.guides.wknd.core.hypermedia.HxRequestState;
import com.adobe.aem.guides.wknd.core.models.BaseFilterModel;
import com.adobe.aem.guides.wknd.core.models.Filter;
import com.adobe.aem.guides.wknd.core.models.HypermediaEventsDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.Option;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;
import com.adobe.aem.guides.wknd.core.models.Search;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Model(
    adaptables = Resource.class,
    adapters = { HypermediaFilterProvider.class, HypermediaPaginationProvider.class },
    resourceType = HypermediaEventsDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaEventsProviderImpl implements HypermediaFilterProvider, HypermediaPaginationProvider {
    private static final String FILTERS_NODE = "filters";
    private static final String PAGE_PARAM = "eventsPage";
    private static final String LIMIT_PARAM = "eventsLimit";

    @Self
    private Resource resource;

    @ValueMapValue
    @Default(values = PaginationConfig.TYPE_PAGES)
    private String paginationType;

    @ValueMapValue
    @Default(intValues = 6)
    private int pageSize;

    @Override
    public BaseFilterModel getFiltersConfig() {
        return getFiltersConfig("");
    }

    @Override
    public BaseFilterModel getFiltersConfig(String paramPrefix) {
        return filtersConfig(resource, paramPrefix);
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request) {
        return getPaginationConfig(request, "");
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request, String paramPrefix) {
        return paginationConfig(resource, request, filteredTotal(resource, request, paramPrefix), paramPrefix);
    }

    static PaginationConfig paginationConfig(Resource resource, SlingHttpServletRequest request, int totalItems, String paramPrefix) {
        ValueMap properties = resource == null ? ValueMap.EMPTY : resource.getValueMap();
        String type = string(properties, "paginationType", PaginationConfig.TYPE_PAGES);
        int size = Math.max(1, properties.get("pageSize", 6));
        return paginationConfig(request, type, PAGE_PARAM, LIMIT_PARAM, size, totalItems, paramPrefix);
    }

    static PaginationConfig paginationConfig(SlingHttpServletRequest request, String type, String pageParam, String limitParam, int pageSize, int totalItems, String paramPrefix) {
        return PaginationConfig.builder()
            .request(request)
            .type(type)
            .pageParam(pageParam)
            .limitParam(limitParam)
            .pageSize(pageSize)
            .totalItems(totalItems)
            .paramPrefix(paramPrefix)
            .build();
    }

    static BaseFilterModel filtersConfig(Resource resource, String paramPrefix) {
        Resource filtersResource = resource == null ? null : resource.getChild(FILTERS_NODE);
        if (filtersResource == null) {
            return emptyConfig();
        }

        String prefix = paramPrefix == null ? "" : paramPrefix;
        List<Filter> filters = new ArrayList<>();
        for (Resource filterResource : filtersResource.getChildren()) {
            ValueMap properties = filterResource.getValueMap();
            String name = string(properties, "name", filterResource.getName());
            String label = string(properties, "label", titleCase(name));
            List<Option> options = options(filterResource, tagIds(properties));
            if (!name.isBlank() && !options.isEmpty()) {
                filters.add(Filter.builder()
                    .type("dropdown")
                    .name(prefix + name)
                    .label(label)
                    .defaultText(string(properties, "defaultText", label))
                    .allText(string(properties, "allText", "All " + label.toLowerCase(Locale.ENGLISH)))
                    .options(options)
                    .classAppend(string(properties, "classAppend", "cmp-events-list-filters__filter--" + name))
                    .build());
            }
        }
        return new BaseFilterModel(filters, Search.disabled());
    }

    private static int filteredTotal(Resource resource, SlingHttpServletRequest request, String paramPrefix) {
        PageManager pageManager = resource == null ? null : resource.getResourceResolver().adaptTo(PageManager.class);
        Page currentPage = pageManager == null ? null : pageManager.getContainingPage(resource);
        if (currentPage == null) {
            return 0;
        }
        List<Filter> filters = filtersConfig(resource, paramPrefix).filters();
        HxRequestState state = HxRequestState.from(request, filters.stream().map(Filter::name).toArray(String[]::new));
        Map<String, String> selectedValues = filters.stream()
            .collect(Collectors.toMap(Filter::name, filter -> state.string(filter.name())));
        List<Page> pages = new ArrayList<>();
        currentPage.listChildren().forEachRemaining(pages::add);
        return (int) pages.stream()
            .filter(page -> matches(page, selectedValues))
            .count();
    }

    private static boolean matches(Page page, Map<String, String> selectedValues) {
        Set<String> pageTags = pageTags(page);
        for (String selectedValue : selectedValues.values()) {
            if (!selectedValue.isBlank() && !pageTags.contains(selectedValue)) {
                return false;
            }
        }
        return true;
    }

    private static Set<String> pageTags(Page page) {
        String[] tags = page.getProperties().get("cq:tags", String[].class);
        if (tags == null) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(tags));
    }

    private static List<Option> options(Resource filterResource, List<String> tagIds) {
        TagManager tagManager = filterResource.getResourceResolver().adaptTo(TagManager.class);
        if (tagManager == null) {
            return List.of();
        }
        return tagIds.stream()
            .map(tagId -> {
                Tag tag = tagManager.resolve(tagId);
                return new Option(tagId, tag == null ? tagId : tag.getTitle());
            })
            .toList();
    }

    private static List<String> tagIds(ValueMap properties) {
        String[] values = properties.get("tags", String[].class);
        if (values == null) {
            values = new String[] { properties.get("tags", "") };
        }
        return Arrays.stream(values)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .toList();
    }

    private static BaseFilterModel emptyConfig() {
        return new BaseFilterModel(List.of(), Search.disabled());
    }

    private static String string(ValueMap properties, String name, String defaultValue) {
        String value = properties.get(name, String.class);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String titleCase(String value) {
        return value == null || value.isBlank()
            ? "Filter"
            : value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1);
    }
}
