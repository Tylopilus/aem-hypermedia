package com.adobe.aem.guides.wknd.core.hypermedia;

public enum HxMethod {
    GET("hx-get"),
    POST("hx-post"),
    PUT("hx-put"),
    PATCH("hx-patch"),
    DELETE("hx-delete");

    private final String attributeName;

    HxMethod(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
