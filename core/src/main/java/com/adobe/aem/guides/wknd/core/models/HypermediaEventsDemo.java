package com.adobe.aem.guides.wknd.core.models;

import java.util.List;

public interface HypermediaEventsDemo extends HypermediaFilterProvider, HypermediaPaginationProvider {
    public static final String RESOURCE_TYPE = "wknd/components/hypermedia-events-demo";

    String getId();
    List<EventItem> getEvents();
    int getTotalEvents();
    int getPageSize();

    public record EventItem(String title, String date, List<String> metadata, String type) {
        public EventItem {
            metadata = metadata == null ? List.of() : List.copyOf(metadata);
        }

        public String meta() { return String.join(" · ", metadata); }
    }
}
