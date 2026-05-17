package com.adobe.aem.guides.wknd.core.models;

import java.util.List;

@lombok.Builder
public record Filter(
    String type,
    String name,
    String label,
    String defaultText,
    String allText,
    List<Option> options,
    String classAppend
) {
    public Filter {
        type = type == null || type.isBlank() ? "dropdown" : type;
        name = name == null ? "" : name;
        label = label == null ? "" : label;
        defaultText = defaultText == null ? "" : defaultText;
        allText = allText == null ? "All" : allText;
        options = options == null ? List.of() : List.copyOf(options);
        classAppend = classAppend == null ? "" : classAppend;
    }
}
