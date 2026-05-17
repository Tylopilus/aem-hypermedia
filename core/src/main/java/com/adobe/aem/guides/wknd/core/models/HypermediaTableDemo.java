package com.adobe.aem.guides.wknd.core.models;

import java.util.List;
import java.util.Map;

public interface HypermediaTableDemo extends HypermediaFilterProvider, HypermediaPaginationProvider {
    public static final String RESOURCE_TYPE = "wknd/components/hypermedia-table-demo";

    String getId();
    List<Header> getHeaders();
    List<Row> getRows();
    int getTotalRows();
    String getSelectedRegion();
    String getSelectedDifficulty();

    public record Header(String label, String fallbackUrl, Map<String, String> attributes, String ariaSort) {}

    public record Row(String adventure, String region, String difficulty, int days) {
        public String getRegionKey() { return region.toLowerCase().replace(' ', '-'); }
        public String getDifficultyKey() { return difficulty.toLowerCase(); }
    }
}
