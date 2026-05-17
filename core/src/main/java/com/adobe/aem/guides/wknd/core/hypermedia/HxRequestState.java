package com.adobe.aem.guides.wknd.core.hypermedia;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.sling.api.SlingHttpServletRequest;

public final class HxRequestState {
    private static final Pattern SAFE_TOKEN = Pattern.compile("[a-z0-9_:/-]{1,128}");
    private final Map<String, List<String>> values;

    private HxRequestState(Map<String, List<String>> values) {
        this.values = values;
    }

    public static HxRequestState from(SlingHttpServletRequest request, String... names) {
        Map<String, List<String>> values = new HashMap<>();
        if (request == null || names == null) {
            return new HxRequestState(values);
        }
        for (String name : names) {
            String[] paramValues = request.getParameterValues(name);
            if (paramValues != null) {
                List<String> normalized = Arrays.stream(paramValues)
                    .map(v -> v.trim().toLowerCase())
                    .filter(v -> SAFE_TOKEN.matcher(v).matches())
                    .toList();
                if (!normalized.isEmpty()) {
                    values.put(name, normalized);
                }
            }
        }
        return new HxRequestState(values);
    }

    public String string(String name) {
        List<String> list = values.getOrDefault(name, List.of());
        return list.isEmpty() ? "" : list.get(0);
    }

    public List<String> strings(String name) {
        return values.getOrDefault(name, List.of());
    }

    public boolean hasValue(String name, String value) {
        return values.getOrDefault(name, List.of()).contains(value);
    }
}
