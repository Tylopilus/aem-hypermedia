package com.adobe.aem.guides.wknd.core.models.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.aem.guides.wknd.core.hypermedia.AbstractHypermediaComponent;
import com.adobe.aem.guides.wknd.core.hypermedia.FragmentCacheMode;
import com.adobe.aem.guides.wknd.core.hypermedia.HxAction;
import com.adobe.aem.guides.wknd.core.hypermedia.HxRequestState;
import com.adobe.aem.guides.wknd.core.models.HypermediaIntroDemo;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = HypermediaIntroDemo.class,
    resourceType = HypermediaIntroDemo.RESOURCE_TYPE,
    defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class HypermediaIntroDemoImpl extends AbstractHypermediaComponent implements HypermediaIntroDemo {
    private static final String TOPIC_PARAM = "introTopic";
    private static final List<Topic> TOPICS = List.of(
        new Topic("html", "1. HTML first", "AEM renders this whole component as normal HTML. The links below are useful before any JavaScript runs."),
        new Topic("fragment", "2. Fragment swap", "With JavaScript enabled, clicking a link asks AEM for this component fragment and swaps only this box."),
        new Topic("fallback", "3. No-JS fallback", "The same links have real href values. If JavaScript is disabled, the browser reloads the page with the selected state in the URL.")
    );

    @ValueMapValue
    @Default(values = "Hypermedia Intro")
    private String title;

    private String activeTopic;

    @PostConstruct
    protected void initModel() {
        String paramName = scopedParam(TOPIC_PARAM);
        String requestedTopic = HxRequestState.from(request, paramName).string(paramName);
        activeTopic = TOPICS.stream()
            .map(Topic::key)
            .filter(requestedTopic::equals)
            .findFirst()
            .orElse(TOPICS.get(0).key());
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getActiveLabel() {
        return activeTopic().label();
    }

    @Override
    public String getMessage() {
        return activeTopic().message();
    }

    @Override
    public List<TopicLink> getTopicLinks() {
        return TOPICS.stream()
            .map(topic -> {
                Map<String, String> state = Map.of(scopedParam(TOPIC_PARAM), topic.key());
                String fallbackUrl = pageUrlWithQuery(state);
                HxAction action = hx()
                    .fallbackUrl(fallbackUrl)
                    .get(fragmentUrlWithQuery(state, FragmentCacheMode.CACHEABLE))
                    .target(componentTarget())
                    .swapOuter()
                    // .pushUrl(fallbackUrl)
                    .build();
                return new TopicLink(topic.label(), action.getFallbackUrl(), action.getAttributes(), topic.key().equals(activeTopic));
            })
            .toList();
    }

    private Topic activeTopic() {
        return TOPICS.stream()
            .filter(topic -> topic.key().equals(activeTopic))
            .findFirst()
            .orElse(TOPICS.get(0));
    }

    private record Topic(String key, String label, String message) {}
}
