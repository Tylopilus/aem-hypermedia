package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.List;
import java.util.function.Function;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.aem.guides.wknd.core.models.BaseFilterModel;
import com.adobe.aem.guides.wknd.core.models.Filter;
import com.adobe.aem.guides.wknd.core.models.HypermediaFilterProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaPaginationProvider;
import com.adobe.aem.guides.wknd.core.models.HypermediaTrainingCatalog;
import com.adobe.aem.guides.wknd.core.models.Option;
import com.adobe.aem.guides.wknd.core.models.PaginationConfig;
import com.adobe.aem.guides.wknd.core.models.Search;
import com.adobe.aem.guides.wknd.core.service.TrainingCatalogService;
import com.adobe.aem.guides.wknd.core.service.TrainingItem;

@Model(
    adaptables = Resource.class,
    adapters = { HypermediaFilterProvider.class, HypermediaPaginationProvider.class },
    resourceType = HypermediaTrainingCatalog.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaTrainingCatalogProviderImpl implements HypermediaFilterProvider, HypermediaPaginationProvider {
    static final String TYPE_PARAM = "type";
    static final String PRODUCT_PARAM = "product";
    static final String LANGUAGE_PARAM = "lang";
    static final String SEARCH_PARAM = "q";
    static final String PAGE_PARAM = "trainingPage";
    static final String LIMIT_PARAM = "trainingLimit";

    @Self
    private Resource resource;

    @OSGiService
    private TrainingCatalogService trainingCatalogService;

    @ValueMapValue
    @Default(values = PaginationConfig.TYPE_PAGES)
    private String paginationType;

    @ValueMapValue
    @Default(intValues = 5)
    private int pageSize;

    @Override
    public BaseFilterModel getFiltersConfig() {
        return getFiltersConfig("");
    }

    @Override
    public BaseFilterModel getFiltersConfig(String paramPrefix) {
        return filtersConfig(trainings(), paramPrefix);
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request) {
        return getPaginationConfig(request, "");
    }

    @Override
    public PaginationConfig getPaginationConfig(SlingHttpServletRequest request, String paramPrefix) {
        return paginationConfig(request, paginationType, pageSize, filteredTotal(trainings(), request, paramPrefix), paramPrefix);
    }

    static BaseFilterModel filtersConfig(List<TrainingItem> trainings, String paramPrefix) {
        String prefix = paramPrefix == null ? "" : paramPrefix;
        return new BaseFilterModel(List.of(
            filter(prefix + TYPE_PARAM, "Training Type", uniqueOptions(trainings, TrainingItem::types)),
            filter(prefix + PRODUCT_PARAM, "Product", uniqueOptions(trainings, TrainingItem::products)),
            filter(prefix + LANGUAGE_PARAM, "Language", uniqueOptions(trainings, TrainingItem::languages))
        ), Search.builder().enabled(true).name(prefix + SEARCH_PARAM).label("Search").build());
    }

    static PaginationConfig paginationConfig(SlingHttpServletRequest request, String type, int pageSize, int totalItems, String paramPrefix) {
        return PaginationConfig.builder()
            .request(request)
            .type(type)
            .pageParam(PAGE_PARAM)
            .limitParam(LIMIT_PARAM)
            .pageSize(pageSize)
            .totalItems(totalItems)
            .paramPrefix(paramPrefix)
            .build();
    }

    static String paginationType(Resource resource) {
        ValueMap properties = resource == null ? ValueMap.EMPTY : resource.getValueMap();
        return properties.get("paginationType", PaginationConfig.TYPE_PAGES);
    }

    static int pageSize(Resource resource) {
        ValueMap properties = resource == null ? ValueMap.EMPTY : resource.getValueMap();
        return Math.max(1, properties.get("pageSize", 5));
    }

    static int filteredTotal(List<TrainingItem> trainings, SlingHttpServletRequest request, String paramPrefix) {
        return filteredTrainings(trainings, request, paramPrefix).size();
    }

    static List<TrainingItem> filteredTrainings(List<TrainingItem> trainings, SlingHttpServletRequest request, String paramPrefix) {
        String prefix = paramPrefix == null ? "" : paramPrefix;
        List<String> typeValues = HypermediaTrainingCatalogImpl.paramValues(request, prefix + TYPE_PARAM);
        List<String> productValues = HypermediaTrainingCatalogImpl.paramValues(request, prefix + PRODUCT_PARAM);
        List<String> languageValues = HypermediaTrainingCatalogImpl.paramValues(request, prefix + LANGUAGE_PARAM);
        String searchQuery = HypermediaTrainingCatalogImpl.paramValue(request, prefix + SEARCH_PARAM);
        return trainings.stream()
            .filter(t -> matchesAny(t.types(), typeValues))
            .filter(t -> matchesAny(t.products(), productValues))
            .filter(t -> matchesAny(t.languages(), languageValues))
            .filter(t -> matchesSearch(t, searchQuery))
            .toList();
    }

    private static Filter filter(String name, String label, List<Option> options) {
        return Filter.builder()
            .type("checkbox")
            .name(name)
            .label(label)
            .options(options)
            .build();
    }

    private static List<Option> uniqueOptions(List<TrainingItem> trainings, Function<TrainingItem, List<String>> extractor) {
        return trainings.stream()
            .flatMap(t -> extractor.apply(t).stream())
            .distinct()
            .sorted()
            .map(v -> new Option(v, capitalize(v)))
            .toList();
    }

    private static boolean matchesAny(List<String> itemValues, List<String> selectedValues) {
        if (selectedValues.isEmpty()) {
            return true;
        }
        return itemValues.stream().anyMatch(selectedValues::contains);
    }

    private static boolean matchesSearch(TrainingItem training, String query) {
        if (query.isBlank()) {
            return true;
        }
        return training.title().toLowerCase().contains(query)
            || training.description().toLowerCase().contains(query);
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private List<TrainingItem> trainings() {
        return trainingCatalogService == null ? List.of() : trainingCatalogService.getTrainings();
    }
}
