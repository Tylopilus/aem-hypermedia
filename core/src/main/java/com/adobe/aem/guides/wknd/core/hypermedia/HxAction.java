package com.adobe.aem.guides.wknd.core.hypermedia;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HxAction {
    private final String fallbackUrl;
    private final Map<String, String> attributes;

    private HxAction(String fallbackUrl, Map<String, String> attributes) {
        this.fallbackUrl = fallbackUrl;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public String getFallbackUrl() {
        return fallbackUrl;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String fallbackUrl = "";
        private HxMethod method;
        private String requestUrl;
        private String target;
        private String select;
        private String swap;
        private String trigger;
        private String pushUrl;
        private String clearParams;
        private String headers;
        private boolean disabled;

        public Builder fallbackUrl(String fallbackUrl) {
            this.fallbackUrl = fallbackUrl == null ? "" : fallbackUrl;
            return this;
        }

        public Builder request(HxMethod method, String requestUrl) {
            this.method = method;
            this.requestUrl = requestUrl;
            return this;
        }

        public Builder get(String requestUrl) {
            return request(HxMethod.GET, requestUrl);
        }

        public Builder post(String requestUrl) {
            return request(HxMethod.POST, requestUrl);
        }

        public Builder target(String target) {
            this.target = target;
            return this;
        }

        public Builder targetSelf() {
            return target("this");
        }

        public Builder select(String select) {
            this.select = select;
            return this;
        }

        public Builder swap(String swap) {
            this.swap = swap;
            return this;
        }

        public Builder swapOuter() {
            return swap("outerHTML");
        }

        public Builder trigger(String trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder pushUrl(String pushUrl) {
            this.pushUrl = pushUrl;
            return this;
        }

        public Builder pushUrl(boolean enabled) {
            this.pushUrl = enabled ? "true" : null;
            return this;
        }

        public Builder clearParams(String clearParams) {
            this.clearParams = clearParams;
            return this;
        }

        public Builder headers(String headers) {
            this.headers = headers;
            return this;
        }

        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public HxAction build() {
            Map<String, String> attributes = new LinkedHashMap<>();
            if (!disabled && method != null && requestUrl != null && !requestUrl.isBlank()) {
                attributes.put(method.getAttributeName(), requestUrl);
                putIfNotBlank(attributes, "hx-target", target);
                putIfNotBlank(attributes, "hx-select", select);
                putIfNotBlank(attributes, "hx-swap", swap);
                putIfNotBlank(attributes, "hx-trigger", trigger);
                putIfNotBlank(attributes, "hx-push-url", pushUrl);
                putIfNotBlank(attributes, "hx-clear-params", clearParams);
                putIfNotBlank(attributes, "hx-headers", headers);
                // Prevent AEM's Link Checker from stripping href/action on mapped URLs
                attributes.put("x-cq-linkchecker", "skip");
            }
            return new HxAction(fallbackUrl, attributes);
        }

        private void putIfNotBlank(Map<String, String> attributes, String name, String value) {
            if (value != null && !value.isBlank()) {
                attributes.put(name, value);
            }
        }
    }
}
