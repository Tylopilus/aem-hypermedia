package com.adobe.aem.guides.wknd.core.models;

import java.util.List;
import java.util.Map;

public interface HypermediaIntroDemo {
    String RESOURCE_TYPE = "wknd/components/hypermedia-intro-demo";

    String getId();
    String getTitle();
    String getActiveLabel();
    String getMessage();
    List<TopicLink> getTopicLinks();

    record TopicLink(String label, String fallbackUrl, Map<String, String> attributes, boolean current) {}
}
