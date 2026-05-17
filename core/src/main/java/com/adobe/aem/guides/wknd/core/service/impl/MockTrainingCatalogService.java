package com.adobe.aem.guides.wknd.core.service.impl;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import com.adobe.aem.guides.wknd.core.service.TrainingCatalogService;
import com.adobe.aem.guides.wknd.core.service.TrainingItem;

@Component(service = TrainingCatalogService.class)
public class MockTrainingCatalogService implements TrainingCatalogService {

    @Override
    public List<TrainingItem> getTrainings() {
        return List.of(
            new TrainingItem(
                "1",
                "AEM Sites Fundamentals",
                "Master the core concepts of AEM Sites including page authoring, component development, and template management.",
                "$499",
                "2 days",
                List.of("online", "workshop"),
                List.of("aem"),
                List.of("en", "de")
            ),
            new TrainingItem(
                "2",
                "Adobe Analytics Deep Dive",
                "Advanced analytics implementation strategies, custom event tracking, and dashboard configuration for enterprise data.",
                "$799",
                "3 days",
                List.of("online", "webinar"),
                List.of("analytics"),
                List.of("en", "fr")
            ),
            new TrainingItem(
                "3",
                "AEM Commerce Integration",
                "Build connected commerce experiences by integrating AEM with Adobe Commerce and third-party platforms.",
                "$999",
                "4 days",
                List.of("in-person", "workshop"),
                List.of("aem", "commerce"),
                List.of("en", "de", "es")
            ),
            new TrainingItem(
                "4",
                "Marketo Engage Masterclass",
                "Email automation, lead scoring, and campaign orchestration using Marketo Engage best practices.",
                "$649",
                "2 days",
                List.of("online"),
                List.of("marketo"),
                List.of("en")
            ),
            new TrainingItem(
                "5",
                "Full Stack AEM Development",
                "Sling Models, OSGi services, frontend pipelines, and cloud deployment patterns for AEM developers.",
                "$1,299",
                "5 days",
                List.of("in-person", "workshop"),
                List.of("aem"),
                List.of("en", "de")
            ),
            new TrainingItem(
                "6",
                "Analytics for Commerce",
                "Track product performance, cart abandonment, and customer journeys across commerce touchpoints.",
                "$749",
                "3 days",
                List.of("online", "webinar"),
                List.of("analytics", "commerce"),
                List.of("en", "es")
            ),
            new TrainingItem(
                "7",
                "AEM Headless Architectures",
                "Design content APIs, GraphQL schemas, and SPA integrations for modern headless delivery.",
                "$899",
                "3 days",
                List.of("online", "workshop"),
                List.of("aem"),
                List.of("en", "fr", "de")
            ),
            new TrainingItem(
                "8",
                "Cross-Cloud Data Pipeline",
                "Unify data across Experience Cloud solutions with real-time event forwarding and identity stitching.",
                "$1,099",
                "4 days",
                List.of("in-person", "webinar"),
                List.of("analytics", "commerce", "marketo"),
                List.of("en", "de")
            ),
            new TrainingItem(
                "9",
                "AEM Accessibility & SEO",
                "Ensure WCAG compliance, structured data, and Core Web Vitals optimization within AEM implementations.",
                "$549",
                "2 days",
                List.of("online"),
                List.of("aem"),
                List.of("en", "es")
            ),
            new TrainingItem(
                "10",
                "Personalization at Scale",
                "Implement audience segmentation, targeted content, and machine-learning powered recommendations.",
                "$849",
                "3 days",
                List.of("online", "in-person"),
                List.of("aem", "analytics", "marketo"),
                List.of("en", "fr")
            ),
            new TrainingItem(
                "11",
                "Commerce Personalization Workshop",
                "Hands-on workshop combining Adobe Commerce with Target and Real-Time CDP for 1:1 experiences.",
                "$1,199",
                "4 days",
                List.of("in-person", "workshop"),
                List.of("commerce", "marketo"),
                List.of("en", "de")
            ),
            new TrainingItem(
                "12",
                "Real-Time CDP Foundations",
                "Data ingestion, profile unification, and activation strategies for Adobe Real-Time Customer Data Platform.",
                "$699",
                "2 days",
                List.of("online", "webinar"),
                List.of("analytics"),
                List.of("en", "es")
            )
        );
    }
}
