package com.adobe.aem.guides.wknd.core.hypermedia;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FragmentUrlService.class)
public class FragmentUrlService {
    public static final String NON_CACHEABLE_HEADER = "X-AEM-Fragment-Cache-Control";

    public String resourceFragmentUrl(Resource resource, HttpServletRequest request) {
        return resourceFragmentUrl(resource, request, FragmentCacheMode.CACHEABLE);
    }

    public String resourceFragmentUrl(Resource resource, HttpServletRequest request, FragmentCacheMode cacheMode) {
        if (resource == null) {
            return "";
        }
        return map(resource.getResourceResolver(), request, resource.getPath() + ".html");
    }

    public String pageUrl(String pagePath, ResourceResolver resourceResolver, HttpServletRequest request) {
        if (pagePath == null || pagePath.isBlank()) {
            return "";
        }
        return map(resourceResolver, request, pagePath + ".html");
    }

    public String appendQuery(String url, Map<String, String> params) {
        return appendQuery(url, params, null);
    }

    public String appendQuery(String url, Map<String, String> singleValueParams, Map<String, List<String>> multiValueParams) {
        if (url == null || url.isBlank()) {
            return url;
        }
        StringBuilder builder = new StringBuilder(url);
        String separator = url.contains("?") ? "&" : "?";

        if (singleValueParams != null) {
            for (Map.Entry<String, String> entry : singleValueParams.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                builder.append(separator)
                    .append(encode(entry.getKey()))
                    .append('=')
                    .append(encode(entry.getValue()));
                separator = "&";
            }
        }

        if (multiValueParams != null) {
            for (Map.Entry<String, List<String>> entry : multiValueParams.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                for (String value : entry.getValue()) {
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    builder.append(separator)
                        .append(encode(entry.getKey()))
                        .append('=')
                        .append(encode(value));
                    separator = "&";
                }
            }
        }

        return builder.toString();
    }

    public String withCurrentQuery(String url, HttpServletRequest request, Map<String, String> overrides) {
        Map<String, String> params = new LinkedHashMap<>();
        if (request != null) {
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });
        }
        if (overrides != null) {
            overrides.forEach((key, value) -> {
                if (value == null || value.isBlank()) {
                    params.remove(key);
                } else {
                    params.put(key, value);
                }
            });
        }
        return appendQuery(url, params);
    }

    public String withCurrentQuery(String url, HttpServletRequest request, Map<String, String> overrides, Map<String, List<String>> multiValueOverrides) {
        Map<String, List<String>> params = new LinkedHashMap<>();
        if (request != null) {
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    params.put(key, Arrays.asList(values));
                }
            });
        }
        if (overrides != null) {
            overrides.forEach((key, value) -> {
                if (value == null || value.isBlank()) {
                    params.remove(key);
                } else {
                    params.put(key, List.of(value));
                }
            });
        }
        if (multiValueOverrides != null) {
            multiValueOverrides.forEach((key, values) -> {
                if (values == null || values.isEmpty()) {
                    params.remove(key);
                } else {
                    List<String> filtered = values.stream()
                        .filter(v -> v != null && !v.isBlank())
                        .toList();
                    if (filtered.isEmpty()) {
                        params.remove(key);
                    } else {
                        params.put(key, filtered);
                    }
                }
            });
        }
        return appendQuery(url, null, params);
    }

    private String map(ResourceResolver resourceResolver, HttpServletRequest request, String url) {
        if (resourceResolver == null) {
            return url;
        }
        if (request == null) {
            return resourceResolver.map(url);
        }
        return resourceResolver.map(request, url);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
