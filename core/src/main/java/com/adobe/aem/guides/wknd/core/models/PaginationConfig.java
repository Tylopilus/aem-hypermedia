package com.adobe.aem.guides.wknd.core.models;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.sling.api.SlingHttpServletRequest;

public record PaginationConfig(
    String type,
    String pageParam,
    String limitParam,
    int pageSize,
    int totalItems,
    int currentPage,
    int currentLimit
) {
    public static final String TYPE_NONE = "none";
    public static final String TYPE_PAGES = "pages";
    public static final String TYPE_LOAD_MORE = "loadMore";

    public static PaginationConfig disabled() {
        return new PaginationConfig(TYPE_NONE, "page", "limit", 0, 0, 1, 0);
    }

    @lombok.Builder(builderClassName = "Builder", builderMethodName = "builder")
    private static PaginationConfig fromRequest(SlingHttpServletRequest request, String type, String pageParam, String limitParam, int pageSize, int totalItems, String paramPrefix) {
        String safeType = type == null || type.isBlank() ? TYPE_PAGES : type;
        String safePageParam = pageParam == null || pageParam.isBlank() ? "page" : pageParam;
        String safeLimitParam = limitParam == null || limitParam.isBlank() ? "limit" : limitParam;
        String prefix = paramPrefix == null ? "" : paramPrefix;
        if (TYPE_NONE.equals(safeType)) {
            return disabled();
        }
        int safePageSize = Math.max(1, pageSize);
        int pageCount = Math.max(1, (int) Math.ceil((double) totalItems / safePageSize));
        int currentPage = Math.min(pageCount, Math.max(1, intParam(request, prefix + safePageParam, 1)));
        int currentLimit = Math.min(totalItems, Math.max(safePageSize, intParam(request, prefix + safeLimitParam, safePageSize)));
        if (TYPE_LOAD_MORE.equals(safeType)) {
            return new PaginationConfig(TYPE_LOAD_MORE, prefix + safePageParam, prefix + safeLimitParam, safePageSize, totalItems, 1, currentLimit);
        }
        return new PaginationConfig(TYPE_PAGES, prefix + safePageParam, prefix + safeLimitParam, safePageSize, totalItems, currentPage, currentLimit);
    }

    private static int intParam(SlingHttpServletRequest request, String name, int defaultValue) {
        try {
            String value = request == null ? null : request.getParameter(name);
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean enabled() {
        return !TYPE_NONE.equals(type) && pageSize > 0 && totalItems > pageSize;
    }

    public boolean pages() {
        return enabled() && TYPE_PAGES.equals(type);
    }

    public boolean loadMore() {
        return enabled() && TYPE_LOAD_MORE.equals(type);
    }

    public int pageCount() {
        return pageSize <= 0 ? 1 : Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
    }

    public boolean hasPreviousPage() {
        return currentPage > 1;
    }

    public boolean hasNextPage() {
        return currentPage < pageCount();
    }

    public boolean hasMore() {
        return currentLimit < totalItems;
    }

    public int nextLimit() {
        return Math.min(totalItems, currentLimit + pageSize);
    }

    public List<Integer> pageNumbers() {
        return IntStream.rangeClosed(1, pageCount()).boxed().toList();
    }

}
