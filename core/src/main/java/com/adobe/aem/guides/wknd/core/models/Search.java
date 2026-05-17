package com.adobe.aem.guides.wknd.core.models;

@lombok.Builder
public record Search(boolean enabled, String name, String label) {
    public Search {
        name = name == null || name.isBlank() ? "q" : name;
        label = label == null || label.isBlank() ? "Search" : label;
    }

    public static Search disabled() {
        return new Search(false, "q", "Search");
    }
}
