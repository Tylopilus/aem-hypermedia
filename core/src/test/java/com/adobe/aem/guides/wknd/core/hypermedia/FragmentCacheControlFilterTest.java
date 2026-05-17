package com.adobe.aem.guides.wknd.core.hypermedia;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FragmentCacheControlFilterTest {

    private FragmentCacheControlFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new FragmentCacheControlFilter();
    }

    @Test
    void setsCachePreventionHeadersWhenNonCacheableHeaderPresent() throws IOException, ServletException {
        when(request.getHeader(FragmentUrlService.NON_CACHEABLE_HEADER)).thenReturn("non-cacheable");

        filter.doFilter(request, response, chain);

        verify(response).setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
        verify(response).setHeader("Surrogate-Control", "no-store");
        verify(response).setHeader("Pragma", "no-cache");
        verify(response).setDateHeader("Expires", 0);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doesNotSetHeadersWhenHeaderAbsent() throws IOException, ServletException {
        when(request.getHeader(FragmentUrlService.NON_CACHEABLE_HEADER)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(response, never()).setHeader(anyString(), anyString());
        verify(response, never()).setDateHeader(anyString(), anyLong());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doesNotSetHeadersWhenHeaderHasDifferentValue() throws IOException, ServletException {
        when(request.getHeader(FragmentUrlService.NON_CACHEABLE_HEADER)).thenReturn("cacheable");

        filter.doFilter(request, response, chain);

        verify(response, never()).setHeader(anyString(), anyString());
        verify(response, never()).setDateHeader(anyString(), anyLong());
        verify(chain).doFilter(request, response);
    }
}
