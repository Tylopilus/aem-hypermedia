package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import com.adobe.aem.guides.wknd.core.models.HypermediaMultiSelectFilter;
import com.adobe.aem.guides.wknd.core.models.HypermediaTrainingCatalog;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = HypermediaMultiSelectFilter.class,
    resourceType = HypermediaMultiSelectFilter.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaMultiSelectFilterImpl implements HypermediaMultiSelectFilter {

    @SlingObject
    private Resource resource;

    private String name;
    private String label;
    private List<HypermediaTrainingCatalog.FilterOption> options;

    @PostConstruct
    @SuppressWarnings("unchecked")
    protected void initModel() {
        ValueMap properties = resource.getValueMap();
        name = properties.get("name", "");
        label = properties.get("label", "");
        options = properties.get("options", List.class);
        if (options == null) {
            options = List.of();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public List<HypermediaTrainingCatalog.FilterOption> getOptions() {
        return options;
    }
}
