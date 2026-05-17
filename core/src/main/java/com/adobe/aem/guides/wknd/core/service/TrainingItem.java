package com.adobe.aem.guides.wknd.core.service;

import java.util.List;

public record TrainingItem(
    String id,
    String title,
    String description,
    String price,
    String duration,
    List<String> types,
    List<String> products,
    List<String> languages
) {
    public TrainingItem {
        types = types == null ? List.of() : List.copyOf(types);
        products = products == null ? List.of() : List.copyOf(products);
        languages = languages == null ? List.of() : List.copyOf(languages);
    }
}
