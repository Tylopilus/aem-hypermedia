package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.adobe.aem.guides.wknd.core.hypermedia.AbstractHypermediaComponent;
import com.adobe.aem.guides.wknd.core.hypermedia.HxRequestState;
import com.adobe.aem.guides.wknd.core.models.BaseFilterModel;
import com.adobe.aem.guides.wknd.core.models.Filter;
import com.adobe.aem.guides.wknd.core.models.HypermediaEventsDemo;
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.Option;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { HypermediaEventsDemo.class, HypermediaFilterProvider.class, HypermediaPaginationProvider.class },
    resourceType = HypermediaEventsDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaEventsDemoImpl extends AbstractHypermediaComponent implements HypermediaEventsDemo {
    @ValueMapValue
    @Default(intValues = 6)
    private int pageSize;

    private List<EventItem> events;
    private int totalEvents;

    @PostConstruct
    protected void initModel() {
        BaseFilterModel filtersConfig = getFiltersConfig();
        List<Filter> filters = filtersConfig.filters();
        HxRequestState state = HxRequestState.from(request, filters.stream().map(Filter::name).toArray(String[]::new));
        Map<String, String> selectedValues = filters.stream()
            .collect(Collectors.toMap(Filter::name, filter -> state.string(filter.name())));
        List<EventItem> matchingEvents = childPages().stream()
            .filter(page -> matches(page, selectedValues))
            .map(page -> eventItem(page, filters))
            .sorted(Comparator.comparing(EventItem::date))
            .toList();
        totalEvents = matchingEvents.size();
        PaginationConfig pagination = getPaginationConfig(request);
        int start = pagination.pages() ? Math.min((pagination.currentPage() - 1) * pagination.pageSize(), totalEvents) : 0;
        int end = pagination.loadMore()
            ? Math.min(pagination.currentLimit(), totalEvents)
            : Math.min(start + pageSize, totalEvents);
        events = matchingEvents.subList(start, end);
    }

    @Override
    public List<EventItem> getEvents() {
        return events;
    }

    @Override
    public int getTotalEvents() {
        return totalEvents;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public BaseFilterModel getFiltersConfig() {
        return HypermediaEventsProviderImpl.filtersConfig(resource, getParamPrefix());
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request) {
        return HypermediaEventsProviderImpl.paginationConfig(resource, request, totalEvents, getParamPrefix());
    }

    private List<Page> childPages() {
        if (currentPage == null) {
            return List.of();
        }
        List<Page> pages = new ArrayList<>();
        currentPage.listChildren().forEachRemaining(pages::add);
        return pages;
    }

    private boolean matches(Page page, Map<String, String> selectedValues) {
        Set<String> pageTags = pageTags(page);
        for (String selectedValue : selectedValues.values()) {
            if (!selectedValue.isBlank() && !pageTags.contains(selectedValue)) {
                return false;
            }
        }
        return true;
    }

    private EventItem eventItem(Page page, List<Filter> filters) {
        ValueMap properties = page.getProperties();
        Set<String> pageTags = pageTags(page);
        List<String> metadata = new ArrayList<>();
        metadata.add(property(properties, "eventDate", ""));
        metadata.addAll(filters.stream()
            .map(filter -> tagLabel(firstMatchingTag(pageTags, filter)))
            .filter(value -> !value.isBlank())
            .toList());
        metadata.add(property(properties, "eventType", "Event"));
        return new EventItem(
            page.getTitle(),
            property(properties, "eventDate", ""),
            metadata,
            property(properties, "eventType", "Event")
        );
    }

    private String firstMatchingTag(Set<String> pageTags, Filter filter) {
        if (filter == null) {
            return "";
        }
        return filter.options().stream()
            .map(Option::value)
            .filter(pageTags::contains)
            .findFirst()
            .orElse("");
    }

    private String tagLabel(String tagId) {
        if (tagId.isBlank()) {
            return "";
        }
        TagManager tagManager = resource.getResourceResolver().adaptTo(TagManager.class);
        Tag tag = tagManager == null ? null : tagManager.resolve(tagId);
        return tag == null ? tagId : tag.getTitle();
    }

    private Set<String> pageTags(Page page) {
        String[] tags = page.getProperties().get("cq:tags", String[].class);
        if (tags == null) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(tags));
    }

    private String property(ValueMap properties, String name, String defaultValue) {
        String value = properties.get(name, String.class);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
