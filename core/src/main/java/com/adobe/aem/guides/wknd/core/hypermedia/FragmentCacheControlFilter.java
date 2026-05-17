package com.adobe.aem.guides.wknd.core.hypermedia;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.engine.EngineConstants;
import org.osgi.service.component.annotations.Component;

/**
 * Sets cache-prevention headers for fragment requests that carry the
 * {@code X-AEM-Fragment-Cache-Control} request header with value {@code non-cacheable}.
 *
 * <p>This keeps fragment URLs clean (no query parameters) while guaranteeing that
 * Dispatcher, CDN and browser will not store personalized or permission-sensitive
 * fragments. The vhost must use {@code Header setifempty} so that these headers
 * are not overwritten by public cache defaults.
 */
@Component(
    service = Filter.class,
    property = {
        EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
        "service.ranking:Integer=" + Integer.MAX_VALUE
    }
)
public class FragmentCacheControlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest
                && response instanceof HttpServletResponse httpResponse
                && "non-cacheable".equals(httpRequest.getHeader(FragmentUrlService.NON_CACHEABLE_HEADER))) {

            httpResponse.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
            httpResponse.setHeader("Surrogate-Control", "no-store");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setDateHeader("Expires", 0);
        }

        chain.doFilter(request, response);
    }
}
