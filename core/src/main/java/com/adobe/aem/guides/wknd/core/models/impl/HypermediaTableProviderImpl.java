package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.List;

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
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaTableDemo;
import com.adobe.aem.guides.wknd.core.models.Option;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;
import com.adobe.aem.guides.wknd.core.models.Search;

@Model(
    adaptables = Resource.class,
    adapters = { HypermediaFilterProvider.class, HypermediaPaginationProvider.class },
    resourceType = HypermediaTableDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaTableProviderImpl implements HypermediaFilterProvider, HypermediaPaginationProvider {
    static final String REGION_PARAM = "tableRegion";
    static final String DIFFICULTY_PARAM = "tableDifficulty";
    private static final String PAGE_PARAM = "tablePage";
    private static final String LIMIT_PARAM = "tableLimit";

    private static final List<Option> REGIONS = List.of(
        new Option("france", "France"),
        new Option("indonesia", "Indonesia"),
        new Option("new-zealand", "New Zealand"),
        new Option("usa", "USA")
    );
    private static final List<Option> DIFFICULTIES = List.of(
        new Option("beginner", "Beginner"),
        new Option("intermediate", "Intermediate"),
        new Option("advanced", "Advanced"),
        new Option("expert", "Expert")
    );

    @Self
    private Resource resource;

    @ValueMapValue
    @Default(values = PaginationConfig.TYPE_PAGES)
    private String paginationType;

    @ValueMapValue
    @Default(intValues = 3)
    private int pageSize;

    @Override
    public BaseFilterModel getFiltersConfig() {
        return getFiltersConfig("");
    }

    @Override
    public BaseFilterModel getFiltersConfig(String paramPrefix) {
        return filtersConfig(paramPrefix);
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request) {
        return getPaginationConfig(request, "");
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request, String paramPrefix) {
        return paginationConfig(resource, request, filteredTotal(request, paramPrefix), paramPrefix);
    }

    static PaginationConfig paginationConfig(Resource resource, SlingHttpServletRequest request, int totalItems, String paramPrefix) {
        ValueMap properties = resource == null ? ValueMap.EMPTY : resource.getValueMap();
        String type = string(properties, "paginationType", PaginationConfig.TYPE_PAGES);
        int size = Math.max(1, properties.get("pageSize", 3));
        return PaginationConfig.builder()
            .request(request)
            .type(type)
            .pageParam(PAGE_PARAM)
            .limitParam(LIMIT_PARAM)
            .pageSize(size)
            .totalItems(totalItems)
            .paramPrefix(paramPrefix)
            .build();
    }

    static BaseFilterModel filtersConfig(String paramPrefix) {
        String prefix = paramPrefix == null ? "" : paramPrefix;
        return new BaseFilterModel(List.of(
            Filter.builder()
                .type("dropdown")
                .name(prefix + REGION_PARAM)
                .label("Region")
                .defaultText("Region")
                .allText("All regions")
                .options(REGIONS)
                .classAppend("cmp-hypermedia-table-filters__filter--region")
                .build(),
            Filter.builder()
                .type("dropdown")
                .name(prefix + DIFFICULTY_PARAM)
                .label("Difficulty")
                .defaultText("Difficulty")
                .allText("All difficulties")
                .options(DIFFICULTIES)
                .classAppend("cmp-hypermedia-table-filters__filter--difficulty")
                .build()
        ), Search.disabled());
    }

    private static int filteredTotal(SlingHttpServletRequest request, String paramPrefix) {
        String prefix = paramPrefix == null ? "" : paramPrefix;
        HxRequestState state = HxRequestState.from(request, prefix + REGION_PARAM, prefix + DIFFICULTY_PARAM);
        String selectedRegion = state.string(prefix + REGION_PARAM);
        String selectedDifficulty = state.string(prefix + DIFFICULTY_PARAM);
        return (int) HypermediaTableDemoImpl.ALL_ROWS.stream()
            .filter(row -> selectedRegion.isBlank() || selectedRegion.equals(row.getRegionKey()))
            .filter(row -> selectedDifficulty.isBlank() || selectedDifficulty.equals(row.getDifficultyKey()))
            .count();
    }

    private static String string(ValueMap properties, String name, String defaultValue) {
        String value = properties.get(name, String.class);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
