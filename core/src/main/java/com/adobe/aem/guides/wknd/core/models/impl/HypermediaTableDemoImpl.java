package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import com.adobe.aem.guides.wknd.core.hypermedia.AbstractHypermediaComponent;
import com.adobe.aem.guides.wknd.core.hypermedia.FragmentCacheMode;
import com.adobe.aem.guides.wknd.core.hypermedia.HxAction;
import com.adobe.aem.guides.wknd.core.hypermedia.HxRequestState;
import com.adobe.aem.guides.wknd.core.models.BaseFilterModel;
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaTableDemo;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { HypermediaTableDemo.class, HypermediaFilterProvider.class, HypermediaPaginationProvider.class },
    resourceType = HypermediaTableDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaTableDemoImpl extends AbstractHypermediaComponent implements HypermediaTableDemo {
    private static final String SORT_PARAM = "tableSort";
    private static final String DIR_PARAM = "tableDir";
    private static final List<String> LABELS = List.of("Adventure", "Region", "Difficulty", "Days");
    static final List<Row> ALL_ROWS = List.of(
        new Row("Bali Surf Camp", "Indonesia", "Beginner", 5),
        new Row("Ski Touring Mont Blanc", "France", "Expert", 7),
        new Row("West Coast Cycling", "USA", "Intermediate", 4),
        new Row("Climbing New Zealand", "New Zealand", "Advanced", 6),
        new Row("Napa Wine Tasting", "USA", "Beginner", 2)
    );
    private int sortIndex;
    private String direction;
    private String selectedRegion;
    private String selectedDifficulty;
    private List<Row> rows;
    private int totalRows;

    @PostConstruct
    protected void initModel() {
        String prefix = getParamPrefix();
        String sortParam = prefix + SORT_PARAM;
        String dirParam = prefix + DIR_PARAM;
        String regionParam = prefix + HypermediaTableProviderImpl.REGION_PARAM;
        String difficultyParam = prefix + HypermediaTableProviderImpl.DIFFICULTY_PARAM;

        HxRequestState state = HxRequestState.from(request, sortParam, dirParam, regionParam, difficultyParam);
        sortIndex = parseSortIndex(state.string(sortParam));
        direction = "desc".equals(state.string(dirParam)) ? "desc" : "asc";
        selectedRegion = state.string(regionParam);
        selectedDifficulty = state.string(difficultyParam);
        rows = ALL_ROWS.stream()
            .filter(row -> selectedRegion.isBlank() || selectedRegion.equals(row.getRegionKey()))
            .filter(row -> selectedDifficulty.isBlank() || selectedDifficulty.equals(row.getDifficultyKey()))
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        rows.sort(comparator(sortIndex));
        if ("desc".equals(direction)) {
            rows.sort(comparator(sortIndex).reversed());
        }
        totalRows = rows.size();
        PaginationConfig pagination = getPaginationConfig(request);
        int start = pagination.pages() ? Math.min((pagination.currentPage() - 1) * pagination.pageSize(), totalRows) : 0;
        int end = pagination.loadMore()
            ? Math.min(pagination.currentLimit(), totalRows)
            : Math.min(start + pagination.pageSize(), totalRows);
        rows = rows.subList(start, end);
    }

    @Override
    public List<Header> getHeaders() {
        String prefix = getParamPrefix();
        List<Header> headers = new ArrayList<>();
        for (int index = 0; index < LABELS.size(); index++) {
            String nextDirection = index == sortIndex && "asc".equals(direction) ? "desc" : "asc";
            Map<String, String> state = Map.of(prefix + SORT_PARAM, String.valueOf(index), prefix + DIR_PARAM, nextDirection);
            String fallbackUrl = pageUrlWithQuery(state);
            HxAction action = hx()
                .fallbackUrl(fallbackUrl)
                .get(fragmentUrlWithQuery(state, FragmentCacheMode.CACHEABLE))
                .target(componentTarget())
                .swapOuter()
                .pushUrl(fallbackUrl)
                .build();
            headers.add(new Header(LABELS.get(index), action.getFallbackUrl(), action.getAttributes(), ariaSort(index)));
        }
        return headers;
    }

    @Override
    public List<Row> getRows() {
        return rows;
    }

    @Override
    public int getTotalRows() {
        return totalRows;
    }

    @Override
    public String getSelectedRegion() {
        return selectedRegion;
    }

    @Override
    public String getSelectedDifficulty() {
        return selectedDifficulty;
    }

    @Override
    public BaseFilterModel getFiltersConfig() {
        return HypermediaTableProviderImpl.filtersConfig(getParamPrefix());
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request) {
        return HypermediaTableProviderImpl.paginationConfig(resource, request, totalRows, getParamPrefix());
    }

    private int parseSortIndex(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 && parsed < LABELS.size() ? parsed : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Comparator<Row> comparator(int index) {
        return switch (index) {
            case 1 -> Comparator.comparing(Row::region);
            case 2 -> Comparator.comparing(Row::difficulty);
            case 3 -> Comparator.comparingInt(Row::days);
            default -> Comparator.comparing(Row::adventure);
        };
    }

    private String ariaSort(int index) {
        if (index != sortIndex) {
            return "none";
        }
        return "desc".equals(direction) ? "descending" : "ascending";
    }
}
